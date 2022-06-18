package bot

import SLErgs.boxes.{SLELendBox, SLERepaymentBox, SLEServiceBox}
import SLErgs.contracts.{SLELendBoxContract, SLERepaymentBoxContract}
import bot.TxType.{CreateLoan, FundLoan, FundRepayment, RefundLoan, TxType}
import cats.Show
import commons.configs.Configs
import commons.node.Client
import db.dbHandlers.DAO
import db.models.ProxyReq
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoContract, InputBox}

import javax.inject.Inject
import scala.collection.mutable

// <editor-fold description="TxGatherer">
/**
  * 1. Takes all txs from blockchain, and create requests
  * 2. Create a TxRequest and enter it into TxQueue
  * 3. TxQueue pulls TxRequest from queues and process it
  *    using pattern matching based on the type. (Pass in
  *    function arguments for each Tx type.
  * 4. Grabs the ServiceBox from ServiceBoxCache and create
  *    the signedTransaction.
  * 5. Store the new Service Box into ServiceBoxCache
  * 6. Send the transaction to the blockchain.
  */
final class SLETxGatherer @Inject() (client: Client) extends TxGatherer(client) {

  override var txQueue: mutable.Queue[TxRequest] =
    new mutable.Queue[TxRequest]()

  def gatherRequests(): Unit =
    client.getClient.execute { ctx =>
      txQueue ++= gatherFundedLoans(ctx)
      txQueue ++= gatherFundedRepayments(ctx)
      txQueue ++= gatherLoansForRefund(ctx)
//      txQueue.enqueue(gatherCreateLoans(null): _*)

      println("Gathered Requests")
    }

  // <editor-fold description="Gather Helpers">
  private def gatherFundedLoans(ctx: BlockchainContext): List[TxRequest] = {
    val txContract: TxContract = FundLoanTxContract(ctx)
    val filterConditions: (InputBox) => Boolean = (box: InputBox) => {
      val wrappedBox = new SLELendBox(box)
      val isFunded =
        box.getValue >= wrappedBox.fundingInfoRegister.fundingGoal

      !isFunded
    }

    gatherBoxes(txContract)(filterConditions)
  }

  private def gatherFundedRepayments(
    ctx: BlockchainContext
  ): List[TxRequest] = {
    val txContract: TxContract = FundRepaymentTxContract(ctx)
    val filterConditions: (InputBox) => Boolean = (box: InputBox) => {
      val wrappedBox: SLERepaymentBox = new SLERepaymentBox(box)
      val isFunded =
        box.getValue >= wrappedBox.repaymentDetailsRegister.repaymentAmount

      isFunded
    }

    gatherBoxes(txContract)(filterConditions)
  }

  private def gatherLoansForRefund(ctx: BlockchainContext): List[TxRequest] = {
    val txContract: TxContract = RefundLoanTxContract(ctx)

    val filterConditions: (InputBox) => Boolean = (box: InputBox) => {
      val wrappedBox: SLELendBox = new SLELendBox(box)
      val passedDeadline: Boolean =
        wrappedBox.fundingInfoRegister.deadlineHeight < ctx.getHeight
      val notFunded: Boolean =
        wrappedBox.value < wrappedBox.fundingInfoRegister.fundingGoal
      val shouldRefund: Boolean =
        if (passedDeadline && notFunded) true else false

      shouldRefund
    }

    gatherBoxes(txContract)(filterConditions)
  }

  // </editor-fold>

  override def generateProcessor: TxProcessor = new SLETxProcessor(txQueue)
}

abstract class TxGatherer(client: Client) {
  var txQueue: mutable.Queue[TxRequest]
  def gatherRequests(): Unit

  protected def gatherBoxes(
    txContract: TxContract
  )(filterConditions: (InputBox) => Boolean): List[TxRequest] = {
    val encodedAddress = Configs.addressEncoder
      .fromProposition(txContract.getContract.getErgoTree)
      .get
      .toString
    val boxes: List[InputBox] = client
      .getAllUnspentBox(Address.create(encodedAddress))
      .filter(box => filterConditions(box))

    val txRequests: List[TxRequest] =
      boxes.map(box => txContract.generateTxRequest(box))

    txRequests
  }

  // TODO: Kii Implement this
  protected def gatherCreateLoans(dao: DAO): List[TxRequest] = ???

  def generateProcessor: TxProcessor
}
// </editor-fold>

// <editor-fold description="TxProcessor">
abstract class TxProcessor(txQueue: mutable.Queue[TxRequest]) {

  def processRequests(): Unit =
    processQueue(txQueue)

  protected def processQueue(txQueue: mutable.Queue[TxRequest]): Unit
}

final class SLETxProcessor(txQueue: mutable.Queue[TxRequest])
    extends TxProcessor(txQueue) {

  override protected def processQueue(
    txQueue: mutable.Queue[TxRequest]
  ): Unit = {
    if (txQueue.isEmpty) return

    val txRequest: TxRequest = txQueue.dequeue()

    txRequest match {
      case FundLoanTxRequest(_) =>
        println(
          s"$FundLoan ${txRequest.asInstanceOf[FundLoanTxRequest].input.getId}"
        )
      case FundRepaymentTxRequest(_) => println(FundRepayment)
      case RefundLoanTxRequest(_)    => println(RefundLoan)
      case CreateLoanTxRequest(_)    => println(CreateLoan)
    }
  }
}
// </editor-fold>

/**
  * Keeps the Service box in a cache
  */
class SLEServiceBoxSingletonCache() {

  var singletonServiceBoxCache: mutable.Queue[SLEServiceBox] =
    new mutable.Queue[SLEServiceBox]()
  var isInstantiated: Boolean = false

}

// <editor-fold description="Tx Components">
/**
  * Categorization of Tx for processing purposes
  * @param txType the type of transaction
  * @param input the input that will be used for processing the tx
  */
sealed trait TxRequest

abstract class InputBoxTxRequest(txType: TxType, input: InputBox)
    extends TxRequest

abstract class DBRequestTxRequest(txType: TxType, input: ProxyReq)
    extends TxRequest

final case class FundLoanTxRequest(input: InputBox)
    extends InputBoxTxRequest(txType = FundLoan, input = input)

final case class FundRepaymentTxRequest(input: InputBox)
    extends InputBoxTxRequest(txType = FundRepayment, input = input)

final case class RefundLoanTxRequest(input: InputBox)
    extends InputBoxTxRequest(txType = RefundLoan, input = input)

final case class CreateLoanTxRequest(req: ProxyReq)
    extends DBRequestTxRequest(txType = CreateLoan, input = req)

trait TxContract {
  val txType: TxType
  def getContract: ErgoContract
  def generateTxRequest: (InputBox) => TxRequest
}

final case class FundLoanTxContract(ctx: BlockchainContext) extends TxContract {
  override val txType: TxType = FundLoan
  override def getContract: ErgoContract = SLELendBoxContract.getContract(ctx)

  override def generateTxRequest: InputBox => TxRequest =
    (box: InputBox) => FundLoanTxRequest(box)
}

final case class RefundLoanTxContract(ctx: BlockchainContext) extends TxContract {
  override val txType: TxType = RefundLoan
  override def getContract: ErgoContract = SLELendBoxContract.getContract(ctx)

  override def generateTxRequest: InputBox => TxRequest =
    (box: InputBox) => RefundLoanTxRequest(box)
}

final case class FundRepaymentTxContract(ctx: BlockchainContext) extends TxContract {
  override val txType: TxType = FundRepayment

  override def getContract: ErgoContract =
    SLERepaymentBoxContract.getContract(ctx)

  override def generateTxRequest: InputBox => TxRequest =
    (box: InputBox) => FundRepaymentTxRequest(box)
}


/**
  * Different TxTypes that requires a ServiceBox
  */
object TxType extends Enumeration {
  type TxType = Value
  val FundLoan, FundRepayment, RefundLoan, CreateLoan = Value
}
// </editor-fold>

object Playground extends App {
  implicit val ShowTxContract: Show[TxContract] = {
    (txContract: TxContract) => s"${txContract.txType} Contract"
  }

  val client: Client = new Client()
  client.setClient()
  val txGatherer: TxGatherer = new SLETxGatherer(client = client)
  txGatherer.gatherRequests()
  txGatherer.txQueue += FundLoanTxRequest(null)

  txGatherer.generateProcessor.processRequests()
}
