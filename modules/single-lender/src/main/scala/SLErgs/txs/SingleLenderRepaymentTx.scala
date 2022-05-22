package SLErgs.txs

import SLErgs.boxes.{SLERepaymentBox, SLEServiceBox, SingleLenderFundRepaymentPaymentBox}
import SLErgs.registers.SingleAddressRegister
import commons.ergo.ErgCommons
import commons.errors.{ProveException}
import db.models.FundRepaymentReq
import org.ergoplatform.appkit._

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Tx: SingleLender Fund Repayment
  *
  * Tx takes in input boxes, and uses the info on the input boxes to
  * create respective output boxes
  * @param repaymentBox
  */
class SingleLenderFundRepaymentTx(
  var repaymentBox: InputBox,
  val singleLenderFundRepaymentPaymentBoxes: mutable.Buffer[InputBox]
) extends FundingTx {
  var paymentBox: Option[SingleLenderFundRepaymentPaymentBox] = None

  def getInputBoxes: Seq[InputBox] = {
    val inputBoxes = Seq(repaymentBox) ++ singleLenderFundRepaymentPaymentBoxes
    inputBoxes
  }

  /**
    *
    * @param ctx
    * @return
    */
  def runTx(ctx: BlockchainContext): SignedTransaction = {

    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()

    val wrappedPaymentBox = paymentBox.get
    val wrappedInputRepaymentBox = new SLERepaymentBox(repaymentBox)

    // if funded then return fundedBox, else return repaymentBox with valid fund
    val outputRepaymentBox = wrappedInputRepaymentBox
      .fundBox(wrappedPaymentBox.value - Parameters.MinFee)
      .getOutputBox(ctx, txB)

    val fundRepaymentTx = txB
      .boxesToSpend(getInputBoxes.asJava)
      .fee(ErgCommons.MinMinerFee)
      .outputs(outputRepaymentBox)
      .sendChangeTo(
        Address
          .create(wrappedPaymentBox.singleAddressRegister.address)
          .getErgoAddress
      )
      .build()

    try {
      val signedTx = prover.sign(fundRepaymentTx)
      signedTx
    } catch {
      case e: ProveException => throw new ProveException()
      case e: Throwable      => throw e
    }
  }

  def applyPaymentBoxInfo(singleAddressRegister: SingleAddressRegister): Unit =
    paymentBox = Option.apply(
      new SingleLenderFundRepaymentPaymentBox(
        singleLenderFundRepaymentPaymentBoxes,
        singleAddressRegister
      )
    )
}

object SingleLenderFundRepaymentTx {

  def create(
    repaymentBox: InputBox,
    singleLenderFundRepaymentPaymentBoxes: mutable.Buffer[InputBox],
    singleAddressRegister: SingleAddressRegister
  ): SingleLenderFundRepaymentTx = {
    val singleLenderFundRepaymentTx = new SingleLenderFundRepaymentTx(
      repaymentBox,
      singleLenderFundRepaymentPaymentBoxes
    )
    singleLenderFundRepaymentTx.applyPaymentBoxInfo(singleAddressRegister)

    singleLenderFundRepaymentTx
  }
}

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
class SingleLenderRepaymentFundedTx(
  val serviceBox: InputBox,
  val repaymentBox: InputBox
) extends Tx {

  def getInputBoxes: List[InputBox] =
    List(serviceBox, repaymentBox)

  /**
    *
    * @param inputBoxes
    * @param ctx
    * @return
    */
  def runTx(ctx: BlockchainContext): SignedTransaction = {
    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()
    val wrappedRepaymentBox = new SLERepaymentBox(repaymentBox)
    val wrappedInputServiceBox = new SLEServiceBox(serviceBox)

    val outputBoxes = wrappedInputServiceBox
      .consumeRepaymentBox(wrappedRepaymentBox, ctx, txB)
      .asJava

    // Send change to ErgoLend
    var lendInitiationTx: UnsignedTransaction = null
    if (outputBoxes.size() >= 3) {
      lendInitiationTx = txB
        .boxesToSpend(getInputBoxes.asJava)
        .fee(ErgCommons.MinMinerFee)
        .outputs(outputBoxes.get(0), outputBoxes.get(1), outputBoxes.get(2))
        .sendChangeTo(wrappedRepaymentBox.getLendersAddress)
        .build()
    } else {
      lendInitiationTx = txB
        .boxesToSpend(getInputBoxes.asJava)
        .fee(ErgCommons.MinMinerFee)
        .outputs(outputBoxes.get(0), outputBoxes.get(1))
        .sendChangeTo(wrappedRepaymentBox.getLendersAddress)
        .build()
    }

    try {
      val signedTx = prover.sign(lendInitiationTx)
      signedTx
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw e
      }
    }
  }
}

object SingleRepaymentTxFactory {

  def createLenderFundRepaymentTx(
    repaymentBox: InputBox,
    repaymentPaymentBox: mutable.Buffer[InputBox],
    req: FundRepaymentReq
  ): SingleLenderFundRepaymentTx = {
    val singleLenderFundRepaymentTx =
      new SingleLenderFundRepaymentTx(repaymentBox, repaymentPaymentBox)
    val singleAddressRegister = new SingleAddressRegister(req.userAddress)
    singleLenderFundRepaymentTx.applyPaymentBoxInfo(singleAddressRegister)

    singleLenderFundRepaymentTx
  }

  def createLenderFundRepaymentTx(
    repaymentBox: InputBox,
    repaymentPaymentBoxes: mutable.Buffer[InputBox],
    singleAddressRegister: SingleAddressRegister
  ): SingleLenderFundRepaymentTx = {
    val singleLenderFundRepaymentTx = SingleLenderFundRepaymentTx.create(
      repaymentBox,
      repaymentPaymentBoxes,
      singleAddressRegister
    )

    singleLenderFundRepaymentTx
  }

  def createSingleLenderRepaymentFundedTx(
    serviceBox: InputBox,
    repaymentBox: InputBox
  ): SingleLenderRepaymentFundedTx = {
    val singleLenderRepaymentFundedTx =
      new SingleLenderRepaymentFundedTx(serviceBox, repaymentBox)

    singleLenderRepaymentFundedTx
  }
}
