package features.lend.txs.singleLender

import config.Configs
import ergotools.LendServiceTokens
import errors.proveException
import features.lend.boxes
import features.lend.boxes.{FundsToAddressBox, LendServiceBox, LendingBox, SingleLenderFundLendPaymentBox, SingleLenderInitiationPaymentBox, SingleLenderLendingBox, SingleLenderRepaymentBox}
import features.lend.boxes.registers.{FundingInfoRegister, LendingProjectDetailsRegister, RepaymentDetailsRegister, SingleLenderRegister}
import features.lend.dao.{CreateLendReq, FundLendReq}
import org.ergoplatform.appkit.{BlockchainContext, InputBox, Parameters, SignedTransaction}

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
class SingleLenderLendInitiationTx(val serviceBox: InputBox,
                                   val lendInitiationProxyContractPayment: InputBox) extends Tx {

  def runTx(ctx: BlockchainContext): SignedTransaction = {
    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()

    val inputServiceBox = new LendServiceBox(serviceBox)
    val paymentBox = new SingleLenderInitiationPaymentBox(lendInitiationProxyContractPayment)
    val outputServiceBox = inputServiceBox.createLend(ctx, txB)

    // create outputLendingBox
    val outputLendingBox: SingleLenderLendingBox = SingleLenderLendingBox.createViaPaymentBox(paymentBox)

    val inputBoxes = List(serviceBox, lendInitiationProxyContractPayment).asJava

    val lendInitiationTx = txB.boxesToSpend(inputBoxes)
      .fee(Configs.fee)
      .outputs(outputServiceBox, outputLendingBox.getInitiationOutputBox(ctx, txB))
      .sendChangeTo(outputLendingBox.getBorrowersAddress)
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

class SingleLenderFundLendBoxTx(var lendingBox: InputBox, val singleLenderFundLendPaymentBox: InputBox) extends Tx {
  /**
   *
   * @param inputBoxes LendingBox + ProxyContract
   * @param ctx
   * @return
   */
  def runTx(ctx: BlockchainContext): SignedTransaction = {

    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()

    val fundLendPaymentBox = new SingleLenderFundLendPaymentBox(singleLenderFundLendPaymentBox)
    val inputLendingBox = new SingleLenderLendingBox(lendingBox)
    val outputLendingBox = inputLendingBox.funded(fundLendPaymentBox.singleLenderRegister.lendersAddress).getFundedOutputBox(ctx, txB)

    val inputBoxes = List(lendingBox, singleLenderFundLendPaymentBox).asJava

    val lendInitiationTx = txB.boxesToSpend(inputBoxes)
      .fee(Configs.fee)
      .outputs(outputLendingBox)
      .sendChangeTo(inputLendingBox.getLendersAddress)
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

class SingleLenderLendBoxFundedTx(val serviceBox: InputBox, var lendingBox: InputBox) extends Tx {
  def getRepaymentBox(fundedHeight: Long, lendingBox: SingleLenderLendingBox): SingleLenderRepaymentBox = {
    val repaymentBox = new SingleLenderRepaymentBox(
      fundingInfoRegister = lendingBox.fundingInfoRegister,
      lendingProjectDetailsRegister = lendingBox.lendingProjectDetailsRegister,
      singleLenderRegister = lendingBox.singleLenderRegister,
      repaymentDetailsRegister = RepaymentDetailsRegister.apply(fundedHeight, lendingBox.fundingInfoRegister))

    repaymentBox
  }

  /**
   *
   * @param inputBoxes lendingBox
   * @param ctx
   * @return
   */
  def runTx(ctx: BlockchainContext): SignedTransaction = {
    val inputLendingBox = new SingleLenderLendingBox(lendingBox)
    val repaymentBox = getRepaymentBox(ctx.getHeight, inputLendingBox)

    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()

    val wrappedServiceBox = new LendServiceBox(serviceBox)
    val outputServiceBox = wrappedServiceBox.fundedLend(ctx, txB)
    val outputRepaymentBox = repaymentBox.getOutputBox(ctx, txB)
    // @todo funds to address box
    val outputFundedBorrowerBox = new FundsToAddressBox(
      value = inputLendingBox.value,
      inputLendingBox.getBorrowersAddress).getOutputBox(txB)

    val inputBoxes = List(serviceBox, lendingBox).asJava

    // Change is send back to lender
    val lendInitiationTx = txB.boxesToSpend(inputBoxes)
      .fee(Configs.fee)
      .outputs(outputServiceBox, outputRepaymentBox, outputFundedBorrowerBox)
      .sendChangeTo(repaymentBox.getLendersAddress)
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
 * As there can only be funded or not funded at all. This will just return creation fee to
 * borrower
 * @param serviceBox
 * @param lendingBox
 */
class SingleLenderRefundLendBoxTx(val serviceBox: InputBox, var lendingBox: InputBox) extends Tx {
  /**
   *
   * @param inputBoxes lendingBox
   * @param ctx
   * @return
   */
  def runTx(ctx: BlockchainContext): SignedTransaction = {
    val wrappedInputLendingBox = new SingleLenderLendingBox(lendingBox)
    val wrappedInputServiceBox = new LendServiceBox(serviceBox)

    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()

    val outputServiceBox = wrappedInputServiceBox.refundLend(ctx, txB)
    val refundToBorrowerPaymentBox =
      new FundsToAddressBox(wrappedInputLendingBox.value - Parameters.MinFee, wrappedInputLendingBox.getBorrowersAddress)
        .getOutputBox(txB)

    val inputBoxes = List(serviceBox, lendingBox).asJava

    // Change is send back to lender
    val lendInitiationTx = txB.boxesToSpend(inputBoxes)
      .fee(Configs.fee)
      .outputs(outputServiceBox, refundToBorrowerPaymentBox)
      .sendChangeTo(wrappedInputLendingBox.getBorrowersAddress)
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

object SingleLenderTxFactory {
  def createLendInitiationTx(serviceBox: InputBox, singleLenderPaymentBox: InputBox): SingleLenderLendInitiationTx = {
    val lendInitiationTx = new SingleLenderLendInitiationTx(serviceBox, singleLenderPaymentBox)

    lendInitiationTx
  }

  /**
   * Note: We don't need to consider if the amount is insufficient. It is integrated into contract
   * @param fundLendReq
   * @param lendingBox
   * @return
   */
  def createFundingLendingBoxTx(lendingBox: InputBox, singleLenderFundPaymentBox: InputBox): SingleLenderFundLendBoxTx = {
    val singleLenderFundLendBoxTx = new SingleLenderFundLendBoxTx(lendingBox, singleLenderFundPaymentBox)

    singleLenderFundLendBoxTx
  }

  def createFundedLendingBoxTx(serviceBox: InputBox, lendingBox: InputBox): SingleLenderLendBoxFundedTx = {
    val singleLenderLendBoxFundedTx = new SingleLenderLendBoxFundedTx(serviceBox, lendingBox)

    singleLenderLendBoxFundedTx
  }

  def createRefundLendBoxTx(serviceBox: InputBox, lendingBox: InputBox): SingleLenderRefundLendBoxTx = {
    val singleLenderRefundLendBoxTx = new SingleLenderRefundLendBoxTx(serviceBox, lendingBox)

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

abstract class FundingTx extends Txs {
}
