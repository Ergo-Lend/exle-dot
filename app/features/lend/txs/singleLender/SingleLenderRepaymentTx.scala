package features.lend.txs.singleLender

import config.Configs
import errors.proveException
import features.lend.boxes.registers.SingleAddressRegister
import features.lend.boxes.{LendServiceBox, PaymentBox, SingleLenderFundRepaymentPaymentBox, SingleLenderRepaymentBox}
import features.lend.dao.FundRepaymentReq
import org.ergoplatform.appkit.{Address, BlockchainContext, InputBox, Parameters, SignedTransaction}

import scala.collection.JavaConverters._

/**
 * Tx: SingleLender Fund Repayment
 *
 * Tx takes in input boxes, and uses the info on the input boxes to
 * create respective output boxes
 * @param repaymentBox
 */
class SingleLenderFundRepaymentTx(var repaymentBox: InputBox,
                                  val singleLenderFundRepaymentPaymentBox: InputBox) extends FundingTx {
  var paymentBox: Option[SingleLenderFundRepaymentPaymentBox] = None

  def getInputBoxes: List[InputBox] = {
    List(repaymentBox, singleLenderFundRepaymentPaymentBox)
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
    val wrappedInputRepaymentBox = new SingleLenderRepaymentBox(repaymentBox)

    // if funded then return fundedBox, else return repaymentBox with valid fund
    val outputRepaymentBox = wrappedInputRepaymentBox.fundBox(singleLenderFundRepaymentPaymentBox.getValue - Parameters.MinFee).getOutputBox(ctx, txB)

    val fundRepaymentTx = txB.boxesToSpend(getInputBoxes.asJava)
      .fee(Configs.fee)
      .outputs(outputRepaymentBox)
      .sendChangeTo(Address.create(wrappedPaymentBox.singleAddressRegister.address).getErgoAddress)
      .build()

    try {
      val signedTx = prover.sign(fundRepaymentTx)
      signedTx
    } catch {
      case e: Throwable =>
        throw e
    }
  }

  def applyPaymentBoxInfo(singleAddressRegister: SingleAddressRegister): Unit = {
    paymentBox = Option.apply(new SingleLenderFundRepaymentPaymentBox(
      singleLenderFundRepaymentPaymentBox.getValue,
      singleAddressRegister))
  }
}

object SingleLenderFundRepaymentTx {
  def create(repaymentBox: InputBox,
             singleLenderFundRepaymentPaymentBox: InputBox,
             singleAddressRegister: SingleAddressRegister): SingleLenderFundRepaymentTx = {
    val singleLenderFundRepaymentTx = new SingleLenderFundRepaymentTx(repaymentBox, singleLenderFundRepaymentPaymentBox)
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
class SingleLenderRepaymentFundedTx(val serviceBox: InputBox, val repaymentBox: InputBox) extends Tx {

  def getInputBoxes: List[InputBox] = {
    List(serviceBox, repaymentBox)
  }

  /**
   *
   * @param inputBoxes
   * @param ctx
   * @return
   */
  def runTx(ctx: BlockchainContext): SignedTransaction = {
    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()
    val wrappedRepaymentBox = new SingleLenderRepaymentBox(repaymentBox)
    val wrappedInputServiceBox = new LendServiceBox(serviceBox)

    val outputServiceBox = wrappedInputServiceBox.consumeRepaymentBox(wrappedRepaymentBox, ctx, txB).asJava
    val ergoLendInterest = wrappedInputServiceBox.profitSharingPercentage.profitSharingPercentage * wrappedRepaymentBox.repaymentDetailsRegister.totalInterestAmount / 100
    val outputLendersPaymentBox = wrappedRepaymentBox.
      repaidLendersPaymentBox(ergoLendInterest).
      getOutputBox(ctx, txB)

    // Send change to ErgoLend
    val lendInitiationTx = txB.boxesToSpend(getInputBoxes.asJava)
      .fee(Configs.fee)
      .outputs(outputServiceBox.get(0), outputServiceBox.get(1), outputLendersPaymentBox)
      .sendChangeTo(wrappedRepaymentBox.getLendersAddress)
      .build()

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
  def createLenderFundRepaymentTx(repaymentBox: InputBox,
                                  repaymentPaymentBox: InputBox,
                                  req: FundRepaymentReq): SingleLenderFundRepaymentTx = {
    val singleLenderFundRepaymentTx = new SingleLenderFundRepaymentTx(repaymentBox, repaymentPaymentBox)
    val singleAddressRegister = new SingleAddressRegister(req.userAddress)
    singleLenderFundRepaymentTx.applyPaymentBoxInfo(singleAddressRegister)

    singleLenderFundRepaymentTx
  }

  def createLenderFundRepaymentTx(repaymentBox: InputBox,
                                  repaymentPaymentBox: InputBox,
                                  singleAddressRegister: SingleAddressRegister): SingleLenderFundRepaymentTx = {
    val singleLenderFundRepaymentTx = SingleLenderFundRepaymentTx.create(repaymentBox, repaymentPaymentBox, singleAddressRegister)

    singleLenderFundRepaymentTx
  }

  def createSingleLenderRepaymentFundedTx(serviceBox: InputBox, repaymentBox: InputBox): SingleLenderRepaymentFundedTx = {
    val singleLenderRepaymentFundedTx = new SingleLenderRepaymentFundedTx(serviceBox, repaymentBox)

    singleLenderRepaymentFundedTx
  }
}