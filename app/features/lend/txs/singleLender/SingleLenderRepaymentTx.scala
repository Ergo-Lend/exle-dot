package features.lend.txs.singleLender

import config.Configs
import errors.proveException
import features.lend.boxes.{LendServiceBox, SingleLenderFundRepaymentPaymentBox, SingleLenderRepaymentBox}
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

    val wrappedPaymentBox = new SingleLenderFundRepaymentPaymentBox(singleLenderFundRepaymentPaymentBox)
    val wrappedInputRepaymentBox = new SingleLenderRepaymentBox(repaymentBox)

    val totalFundedValue = wrappedInputRepaymentBox.value + wrappedPaymentBox.value
    val isFunded = totalFundedValue >= wrappedInputRepaymentBox.repaymentDetailsRegister.repaymentAmount + Parameters.MinFee

    // if funded then return fundedBox, else return repaymentBox with valid fund
    val outputRepaymentBox = if (isFunded) wrappedInputRepaymentBox.fundedBox().getOutputBox(ctx, txB)
      else wrappedInputRepaymentBox.fundBox(singleLenderFundRepaymentPaymentBox.getValue - Parameters.MinFee)
      .getOutputBox(ctx, txB)

    val lendInitiationTx = txB.boxesToSpend(getInputBoxes.asJava)
      .fee(Configs.fee)
      .outputs(outputRepaymentBox)
      .sendChangeTo(Address.create(wrappedPaymentBox.singleAddressRegister.address).getErgoAddress)
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
    val ergoLendInterest = wrappedInputServiceBox.profitSharingPercentage.value * wrappedRepaymentBox.repaymentDetailsRegister.totalInterestAmount
    val outputLendersPaymentBox = wrappedRepaymentBox.repaidLendersPaymentBox(ergoLendInterest).getOutputBox(ctx, txB)

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
        throw proveException()
      }
    }
  }
}

object SingleRepaymentTxFactory {
  def createLenderFundRepaymentTx(repaymentBox: InputBox, repaymentPaymentBox: InputBox): SingleLenderFundRepaymentTx = {
    val singleLenderFundRepaymentTx = new SingleLenderFundRepaymentTx(repaymentBox, repaymentPaymentBox)

    singleLenderFundRepaymentTx
  }

  def createSingleLenderRepaymentFundedTx(serviceBox: InputBox, repaymentBox: InputBox): SingleLenderRepaymentFundedTx = {
    val singleLenderRepaymentFundedTx = new SingleLenderRepaymentFundedTx(serviceBox, repaymentBox)

    singleLenderRepaymentFundedTx
  }
}