package SLErgs.txs

import SLErgs.boxes.{
  FundsToAddressBox,
  SLELendBox,
  SLERepaymentBox,
  SLEServiceBox,
  SingleLenderFundLendPaymentBox,
  SingleLenderInitiationPaymentBox
}
import commons.registers.{
  BorrowerRegister,
  FundingInfoRegister,
  LendingProjectDetailsRegister,
  RepaymentDetailsRegister,
  SingleLenderRegister
}
import commons.configs.ServiceConfig
import commons.ergo.ErgCommons
import commons.errors.{PaymentBoxInfoNotFoundException, ProveException}
import db.models.{CreateLendReq, FundLendReq}
import org.ergoplatform.appkit.{
  BlockchainContext,
  InputBox,
  Parameters,
  SignedTransaction
}

import scala.collection.JavaConverters._
import scala.collection.mutable

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
class SingleLenderLendInitiationTx(
  val serviceBox: InputBox,
  val lendInitiationProxyContractPayment: mutable.Buffer[InputBox]
) extends FundingTx {
  var paymentBox: Option[SingleLenderInitiationPaymentBox] = None

  def runTx(ctx: BlockchainContext): SignedTransaction = {
    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()

    val inputServiceBox = new SLEServiceBox(serviceBox)

    if (paymentBox.isEmpty)
      throw PaymentBoxInfoNotFoundException()

    val wrappedOutputServiceBox = inputServiceBox.createLend()
    val outputServiceBox = wrappedOutputServiceBox.getOutputServiceBox(ctx, txB)

    // create outputLendingBox
    val wrappedOutputLendBox: SLELendBox =
      SLELendBox.createViaPaymentBox(paymentBox.get)
    val outputLendBox = wrappedOutputLendBox.getInitiationOutputBox(ctx, txB)
    val outputServiceFeeBox =
      new FundsToAddressBox(
        ServiceConfig.serviceFee,
        ServiceConfig.serviceOwner.toString
      ).getOutputBox(txB)

    val inputBoxes = Seq(serviceBox) ++ lendInitiationProxyContractPayment

    val lendInitiationTx = txB
      .boxesToSpend(inputBoxes.asJava)
      .fee(Parameters.MinFee)
      .outputs(outputServiceBox, outputLendBox, outputServiceFeeBox)
      .sendChangeTo(wrappedOutputLendBox.getBorrowersAddress.getErgoAddress)
      .build()

    try {
      val signedTx = prover.sign(lendInitiationTx)
      signedTx
    } catch {
      case e: Throwable => {
        throw ProveException(additionalInfo = e.toString)
      }
    }
  }

  def applyPaymentBoxInfo(
    fundingInfoRegister: FundingInfoRegister,
    lendingProjectDetailsRegister: LendingProjectDetailsRegister,
    borrowerRegister: BorrowerRegister
  ): Unit =
    paymentBox = Option.apply(
      new SingleLenderInitiationPaymentBox(
        lendInitiationProxyContractPayment,
        fundingInfoRegister,
        lendingProjectDetailsRegister,
        borrowerRegister
      )
    )
}

class SingleLenderFundLendBoxTx(
  var lendingBox: InputBox,
  val singleLenderFundLendPaymentBoxes: mutable.Buffer[InputBox]
) extends FundingTx {
  var paymentBox: Option[SingleLenderFundLendPaymentBox] = None

  /**
    *
    * @param inputBoxes LendingBox + ProxyContract
    * @param ctx
    * @return
    */
  def runTx(ctx: BlockchainContext): SignedTransaction = {

    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()

    val fundLendPaymentBox = paymentBox.get
    val inputLendBox = new SLELendBox(lendingBox)
    val wrappedOutputLendBox = inputLendBox.fundBox(
      fundLendPaymentBox.singleLenderRegister.lendersAddress
    )
    val outputLendBox = wrappedOutputLendBox.getOutputBox(ctx, txB)

    val inputBoxes = Seq(lendingBox) ++ singleLenderFundLendPaymentBoxes

    val lendInitiationTx = txB
      .boxesToSpend(inputBoxes.asJava)
      .fee(Parameters.MinFee)
      .outputs(outputLendBox)
      .sendChangeTo(wrappedOutputLendBox.getLendersAddress.getErgoAddress)
      .build()

    try {
      val signedTx = prover.sign(lendInitiationTx)
      signedTx
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw ProveException(additionalInfo = e.toString)
      }
    }
  }

  def applyPaymentBoxInfo(singleLenderRegister: SingleLenderRegister): Unit =
    paymentBox = Option.apply(
      new SingleLenderFundLendPaymentBox(
        singleLenderFundLendPaymentBoxes,
        singleLenderRegister = singleLenderRegister
      )
    )
}

class SLELendBoxFundedTx(val serviceBox: InputBox, var lendBox: InputBox)
    extends Tx {

  def getRepaymentBox(
    fundedHeight: Long,
    lendBox: SLELendBox
  ): SLERepaymentBox = {
    val repaymentBox = new SLERepaymentBox(
      fundingInfoRegister = lendBox.fundingInfoRegister,
      lendingProjectDetailsRegister = lendBox.lendingProjectDetailsRegister,
      borrowerRegister = lendBox.borrowerRegister,
      singleLenderRegister = lendBox.singleLenderRegister,
      repaymentDetailsRegister = RepaymentDetailsRegister
        .apply(fundedHeight, lendBox.fundingInfoRegister)
    )

    repaymentBox
  }

  def getBorrowersFundedBoxValue(inputLendBox: SLELendBox): Long =
    inputLendBox.value - ErgCommons.MinBoxFee - Parameters.MinFee

  /**
    *
    * @param inputBoxes lendingBox
    * @param ctx
    * @return
    */
  def runTx(ctx: BlockchainContext): SignedTransaction = {
    val inputLendBox = new SLELendBox(lendBox)
    val repaymentBox = getRepaymentBox(ctx.getHeight.toLong, inputLendBox)

    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()

    val wrappedServiceBox = new SLEServiceBox(serviceBox)
    val outputServiceBox =
      wrappedServiceBox.fundedLend.getOutputServiceBox(ctx, txB)
    val outputRepaymentBox = repaymentBox.getOutputBox(ctx, txB)

    // @todo funds to address box
    val fundedValue = getBorrowersFundedBoxValue(inputLendBox)
    val outputFundedBorrowerBox = new FundsToAddressBox(
      value = fundedValue,
      inputLendBox.getBorrowersAddress
    ).getOutputBox(txB)

    val inputBoxes = List(serviceBox, lendBox).asJava

    // Change is send back to lender
    val lendFundTx = txB
      .boxesToSpend(inputBoxes)
      .fee(Parameters.MinFee)
      .outputs(outputServiceBox, outputRepaymentBox, outputFundedBorrowerBox)
      .sendChangeTo(repaymentBox.getLendersAddress)
      .build()

    try {
      val signedTx = prover.sign(lendFundTx)
      signedTx
    } catch {
      case e: Throwable => {
        throw ProveException(e.getMessage)
      }
    }
  }
}

/**
  * As there can only be funded or not funded at all. This will just return creation fee to
  * borrower
  * @param serviceBox
  * @param lendBox
  */
class SingleLenderRefundLendBoxTx(
  val serviceBox: InputBox,
  var lendBox: InputBox
) extends Tx {

  /**
    *
    * @param inputBoxes lendBox
    * @param ctx
    * @return
    */
  def runTx(ctx: BlockchainContext): SignedTransaction = {
    val wrappedInputLendingBox = new SLELendBox(lendBox)
    val wrappedInputServiceBox = new SLEServiceBox(serviceBox)

    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()

    val outputServiceBox =
      wrappedInputServiceBox.refundLend.getOutputServiceBox(ctx, txB)
    val inputBoxes = List(serviceBox, lendBox).asJava

    // Change is send back to lender
    val lendInitiationTx = txB
      .boxesToSpend(inputBoxes)
      .fee(ErgCommons.MinMinerFee)
      .outputs(outputServiceBox)
      .sendChangeTo(wrappedInputLendingBox.getBorrowersAddress.getErgoAddress)
      .build()

    try {
      val signedTx = prover.sign(lendInitiationTx)
      signedTx
    } catch {
      case e: Throwable      => throw e
      case e: ProveException => throw new ProveException()
    }
  }
}

object SingleLenderLendInitiationTx {

  def create(
    serviceBox: InputBox,
    lendInitiationContractPayment: mutable.Buffer[InputBox],
    fundingInfoRegister: FundingInfoRegister,
    lendingProjectDetailsRegister: LendingProjectDetailsRegister,
    borrowerRegister: BorrowerRegister
  ): SingleLenderLendInitiationTx = {
    val singleLenderLendInitiationTx = new SingleLenderLendInitiationTx(
      serviceBox,
      lendInitiationContractPayment
    )
    singleLenderLendInitiationTx.applyPaymentBoxInfo(
      fundingInfoRegister,
      lendingProjectDetailsRegister,
      borrowerRegister
    )

    singleLenderLendInitiationTx
  }
}

object SingleLenderFundLendBoxTx {

  def create(
    lendBox: InputBox,
    singleLenderFundLendPaymentBoxes: mutable.Buffer[InputBox],
    singleLenderRegister: SingleLenderRegister
  ): SingleLenderFundLendBoxTx = {
    val singleLenderFundLendBoxTx =
      new SingleLenderFundLendBoxTx(lendBox, singleLenderFundLendPaymentBoxes)
    singleLenderFundLendBoxTx.applyPaymentBoxInfo(singleLenderRegister)

    singleLenderFundLendBoxTx
  }
}

object SingleLenderTxFactory {

  def createLendInitiationTx(
    serviceBox: InputBox,
    singleLenderPaymentBox: mutable.Buffer[InputBox],
    req: CreateLendReq
  ): SingleLenderLendInitiationTx = {
    val lendInitiationTx =
      new SingleLenderLendInitiationTx(serviceBox, singleLenderPaymentBox)
    val fundingInfoRegister = new FundingInfoRegister(
      fundingGoal = req.goal,
      deadlineHeight = req.deadlineHeight,
      interestRatePercent = req.interestRate,
      repaymentHeightLength = req.repaymentHeight,
      creationHeight = req.creationHeight
    )
    val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(
      projectName = req.name,
      description = req.description
    )
    val borrowerRegister = new BorrowerRegister(req.borrowerAddress)

    lendInitiationTx.applyPaymentBoxInfo(
      fundingInfoRegister,
      lendingProjectDetailsRegister,
      borrowerRegister
    )

    lendInitiationTx
  }

  def createLendInitiationTx(
    serviceBox: InputBox,
    singleLenderPaymentBoxes: mutable.Buffer[InputBox],
    fundingInfoRegister: FundingInfoRegister,
    lendingProjectDetailsRegister: LendingProjectDetailsRegister,
    borrowerRegister: BorrowerRegister
  ): SingleLenderLendInitiationTx = {

    val singleLenderLendInitiationTx = SingleLenderLendInitiationTx.create(
      serviceBox,
      singleLenderPaymentBoxes,
      fundingInfoRegister,
      lendingProjectDetailsRegister,
      borrowerRegister
    )

    singleLenderLendInitiationTx
  }

  /**
    * Note: We don't need to consider if the amount is insufficient. It is integrated into contract
    * @param fundLendReq
    * @param lendBox
    * @return
    */
  def createFundingLendBoxTx(
    lendBox: InputBox,
    singleLenderFundPaymentBoxes: mutable.Buffer[InputBox],
    req: FundLendReq
  ): SingleLenderFundLendBoxTx = {
    val singleLenderFundLendBoxTx =
      new SingleLenderFundLendBoxTx(lendBox, singleLenderFundPaymentBoxes)
    val singleLenderRegister = new SingleLenderRegister(req.lenderAddress)

    singleLenderFundLendBoxTx.applyPaymentBoxInfo(singleLenderRegister)

    singleLenderFundLendBoxTx
  }

  def createFundingLendBoxTx(
    lendBox: InputBox,
    singleLenderFundPaymentBoxes: mutable.Buffer[InputBox],
    singleLenderRegister: SingleLenderRegister
  ): SingleLenderFundLendBoxTx = {
    val singleLenderFundLendBoxTx = SingleLenderFundLendBoxTx.create(
      lendBox,
      singleLenderFundPaymentBoxes,
      singleLenderRegister
    )

    singleLenderFundLendBoxTx
  }

  def createFundedLendBoxTx(
    serviceBox: InputBox,
    lendBox: InputBox
  ): SLELendBoxFundedTx = {
    val singleLenderLendBoxFundedTx =
      new SLELendBoxFundedTx(serviceBox, lendBox)

    singleLenderLendBoxFundedTx
  }

  def createRefundLendBoxTx(
    serviceBox: InputBox,
    lendBox: InputBox
  ): SingleLenderRefundLendBoxTx = {
    val singleLenderRefundLendBoxTx =
      new SingleLenderRefundLendBoxTx(serviceBox, lendBox)

    singleLenderRefundLendBoxTx
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
//  def runTx(inputBoxes: Seq[InputBox], blockchainContext: BlockchainContext): SignedTransaction
}

trait Txs

abstract class FundingTx extends Txs {}
