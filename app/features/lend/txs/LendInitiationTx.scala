package features.lend.txs

import config.Configs
import ergotools.client.Client
import errors.proveException
import features.lend.ServiceBox
import features.lend.boxes.{Box, LendingBox}
import features.lend.boxes.registers.{FundingInfoRegister, LendingProjectDetailsRegister}
import features.lend.dao.CreateLendReq
import helpers.StackTrace
import org.ergoplatform.appkit.{Address, BlockchainContext, InputBox, SignedTransaction}

import scala.collection.JavaConverters._

/**
 * Initiates Lending Transaction
 * by creating a lend box from a proxy contract.
 * Takes in a service box and proxy contract and output
 * service box and lending box.
 * 1. Receives Input Boxes
 * 2. Run Transaction
 * 3. Get Output Box
 *
 * Input Boxes:
 * - Service Box
 * - Proxy Contract box
 *
 * Output Box:
 * - Service Box
 * - Lending Box
 */
class LendInitiationTx(
//                        val serviceBox: ServiceBox,
                        var lendingBox: LendingBox) extends Tx {
  var inputBox: List[Box] = _

  override def getOutputBox: Box = {
    lendingBox
  }

  override def runTx(inputBoxes: Seq[InputBox], ctx: BlockchainContext): SignedTransaction = {
    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()
    val outputLendingBox = lendingBox.getOutputBox(ctx)
    // @todo enable service box
//    val outputServiceBox = serviceBox.getOutputBox(ctx)

    val lendInitiationTx = txB.boxesToSpend(inputBoxes.asJava)
      .fee(Configs.fee)
      .outputs(outputLendingBox)
      .sendChangeTo(lendingBox.getBorrowersAddress)
      .build()

    try {
      val signedTx = prover.sign(lendInitiationTx)
      signedTx
    } catch {
      case e: Throwable => {
        throw proveException()
      }
    }
  }

  def insertLendingBoxDetails(inputLendingBox: LendingBox): Unit = {
    lendingBox = inputLendingBox
  }
}

object TxFactory {
  def createLendInitiationTx(req: CreateLendReq): LendInitiationTx = {
    val fundingInfoRegister = new FundingInfoRegister(req.goal, req.deadlineHeight, req.interestRatePercent, req.repaymentHeight)
    val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(req.name, req.description, req.borrowerAddress)
    val lendingBox = new LendingBox(fundingInfoRegister, lendingProjectDetailsRegister)
    val lendInitiationTx = new LendInitiationTx(lendingBox)

    lendInitiationTx
  }
}

/**
 * Transactions
 * The goal of a transaction class is to keep the transaction:
 * 1. explicit
 * 2. simple and straightforward to use (within its explicitness)
 *
 * Steps:
 * 1. Input boxes
 * 2. Run Tx
 * 3. Get Output box
 *
 * Explicitness
 * explicitly announcing that this transaction is a LendInitiation Tx or if
 * this tx is a RepaymentTx. The explicitness of the transaction reduce
 * confusion for engineers when calling a transaction.
 * It also makes the code more readable
 */
abstract class Tx {
  def getOutputBox: Box
  def runTx(inputBoxes: Seq[InputBox], blockchainContext: BlockchainContext): SignedTransaction
}