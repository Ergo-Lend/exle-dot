package SLTokens.txs

import SLErgs.boxes.FundsToAddressBox
import SLTokens.SLTTokens
import SLTokens.boxes.{SLTLendBox, SLTRepaymentBox, SLTServiceBox}
import commons.configs.ServiceConfig
import org.ergoplatform.appkit.{
  BlockchainContext,
  InputBox,
  OutBox,
  SignedTransaction,
  UnsignedTransaction,
  UnsignedTransactionBuilder
}
import txs.Tx

// <editor-fold desc="SLT LEND INITIATION TX">
/**
  * // ============== SLT LEND INITIATION TX ================= //
  * @param inputBoxes 1. Service Box, 2. Creation Payment Box
  *                   (In our case, we have to ensure that payment box has registers filled)
  * @param ctx Blockchain Context
  */
case class SLTLendInitiationTx(inputBoxes: Seq[InputBox])(
  implicit val ctx: BlockchainContext
) extends Tx {

  /**
    * Checks if the first box is an SLTService box
    * @param inputBoxes input boxes that will be used for the txs
    * @return
    */
  def checkInputBoxes(inputBoxes: Seq[InputBox]): Boolean =
    try {
      val serviceBox: SLTServiceBox = new SLTServiceBox(inputBoxes.head)
      serviceBox.tokens.head.getId == SLTTokens.serviceNFTId
    } catch {
      case _: Throwable => false
    }

  /**
    * @return OutBox: 1. ServiceBox, 2. SLTLendBox, 3. ServiceFee
    */
  override def getOutBoxes: Seq[OutBox] =
    if (!checkInputBoxes(inputBoxes)) throw new IllegalArgumentException()
    else {
      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
      val paymentBox: InputBox = inputBoxes(1)
      val wrappedSLTServiceBox: SLTServiceBox = new SLTServiceBox(
        inputBoxes.head
      )

      val wrappedSLTServiceOutBox: SLTServiceBox =
        SLTServiceBox.createLendBox(wrappedSLTServiceBox)
      val wrappedSLTLendBox: SLTLendBox =
        SLTLendBox.fromCreatePaymentBox(paymentBox)
      val serviceFeeOutBox: OutBox = FundsToAddressBox(
        value = ServiceConfig.serviceFee,
        address = ServiceConfig.serviceOwner
      ).getOutputBox(ctx, txB)

      Seq(
        wrappedSLTServiceOutBox.getOutBox(ctx, txB),
        wrappedSLTLendBox.getOutBox(ctx, txB),
        serviceFeeOutBox
      )
    }
}
// </editor-fold>

// <editor-fold desc="SLT LEND FUND TX">
/**
  * // ================== SLT LEND FUND TX ================ //
  * @param inputBoxes 1. SLTLendBox 2. Fund Lend Payment Box
  *                   (Payment Box would have to have 1st Register with lender Id)
  */
case class SLTLendFundTx(inputBoxes: Seq[InputBox])(
  implicit val ctx: BlockchainContext
) extends Tx {

  /**
    * SLTLendBox check
    * 1. Does it have the lend box token
    * @param inputBoxes 1. SLTLendBox 2. Fund Lend Payment Box
    * @return
    */
  def checkInputBoxes(inputBoxes: Seq[InputBox]): Boolean =
    try {
      val lendBox: SLTLendBox = new SLTLendBox(inputBoxes.head)
      lendBox.tokens.head.getId == SLTTokens.lendTokenId
    } catch {
      case _: Throwable => false
    }

  override def getOutBoxes: Seq[OutBox] =
    if (!checkInputBoxes(inputBoxes)) throw new IllegalArgumentException()
    else {
      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
      val paymentBox: InputBox = inputBoxes(1)
      val wrappedSLTLendBox: SLTLendBox = new SLTLendBox(inputBoxes.head)

      val wrappedOutSLTLendBox: SLTLendBox =
        SLTLendBox.getFunded(wrappedSLTLendBox, paymentBox)

      Seq(wrappedOutSLTLendBox.getOutBox(ctx, txB))
    }
}
// </editor-fold>

// <editor-fold desc="SLT REPAYMENT FUND TX">
/**
  * // ================== SLT REPAYMENT FUND TX ================ //
  * @param inputBoxes 1. SLTRepaymentBox 2. Fund Repayment Payment Box
  *                   (Repayment box does not need any registers filled)
  */
case class SLTRepaymentFundTx(inputBoxes: Seq[InputBox])(
  implicit val ctx: BlockchainContext
) extends Tx {

  /**
    * SLTLendBox check
    * 1. Does it have the lend box token
    * @param inputBoxes 1. SLTLendBox 2. Fund Lend Payment Box
    * @return
    */
  def checkInputBoxes(inputBoxes: Seq[InputBox]): Boolean =
    try {
      val lendBox: SLTRepaymentBox = new SLTRepaymentBox(inputBoxes.head)
      lendBox.tokens.head.getId == SLTTokens.repaymentTokenId
    } catch {
      case _: Throwable => false
    }

  override def getOutBoxes: Seq[OutBox] =
    if (!checkInputBoxes(inputBoxes)) throw new IllegalArgumentException()
    else {
      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
      val paymentBox: InputBox = inputBoxes(1)
      val wrappedSLTLendBox: SLTRepaymentBox = new SLTRepaymentBox(
        inputBoxes.head
      )

      val wrappedOutSLTLendBox: SLTRepaymentBox =
        SLTRepaymentBox.fundBox(wrappedSLTLendBox, paymentBox)

      Seq(wrappedOutSLTLendBox.getOutBox(ctx, txB))
    }
}
// </editor-fold>

// <editor-fold desc="SLT LEND FUND SUCCESS TX">
// </editor-fold>

// <editor-fold desc="SLT REPAYMENT FUND SUCCESS TX">
// </editor-fold>

// <editor-fold desc="SLT REFUND TX">
// </editor-fold>
