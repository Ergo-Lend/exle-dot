package SLTokens.txs

import SLTokens.SLTTokens
import SLTokens.boxes.{SLTCreateLendProxyBox, SLTFundLendProxyBox, SLTFundRepaymentProxyBox, SLTLendBox, SLTRepaymentBox, SLTRepaymentDistribution, SLTServiceBox}
import boxes.FundsToAddressBox
import commons.configs.ServiceConfig
import org.ergoplatform.appkit.{BlockchainContext, ErgoId, ErgoToken, InputBox, OutBox, UnsignedTransactionBuilder}
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
      val paymentBox: SLTCreateLendProxyBox = new SLTCreateLendProxyBox(inputBoxes(1))
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
      ).getOutBox(ctx, txB)

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
    * @todo kii, there's something wrong with this
    *       Probably will cause issues with others too
    * 1. Does it have the lend box token
    * @param inputBoxes 1. SLTLendBox 2. Fund Lend Payment Box
    * @return
    */
  def checkInputBoxes(inputBoxes: Seq[InputBox]): Boolean =
    try {
      val lendBox: SLTLendBox = new SLTLendBox(inputBoxes.head)
      val lendTokens: Seq[ErgoToken] = lendBox.tokens
      val lendToken: ErgoToken = lendTokens.head
      val lendTokenId: ErgoId = lendToken.getId
      lendTokenId.equals(SLTTokens.lendTokenId)
    } catch {
      case e: Exception => throw new Exception(e.getMessage)
      case _: Throwable => throw new Throwable()
    }

  override def getOutBoxes: Seq[OutBox] = {
    val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
    val paymentBox: SLTFundLendProxyBox = new SLTFundLendProxyBox(inputBoxes(1))
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
case class SLTRepaymentFundTx(
  inputBoxes: Seq[InputBox],
  override val dataInputs: Seq[InputBox]
)(
  implicit val ctx: BlockchainContext
) extends Tx {

  /**
    * SLTRepaymentBox check
    * 1. Does it have the repayment box token
    * @param inputBoxes 1. SLTRepaymentBox 2. Fund Repayment Payment Box
    * @return
    */
  def checkInputBoxes(inputBoxes: Seq[InputBox]): Boolean =
    try {
      val repaymentBox: SLTRepaymentBox = new SLTRepaymentBox(inputBoxes.head)
      repaymentBox.tokens.head.getId == SLTTokens.repaymentTokenId
    } catch {
      case _: Throwable => false
    }

  override def getOutBoxes: Seq[OutBox] = {
    val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
    val paymentBox: SLTFundRepaymentProxyBox = new SLTFundRepaymentProxyBox(inputBoxes(1))
    val wrappedSLTRepaymentBox: SLTRepaymentBox = new SLTRepaymentBox(
      inputBoxes.head
    )

    val wrappedOutSLTRepaymentBox: SLTRepaymentBox =
      SLTRepaymentBox.fundBox(wrappedSLTRepaymentBox, paymentBox)

    Seq(wrappedOutSLTRepaymentBox.getOutBox(ctx, txB))
  }
}
// </editor-fold>

// <editor-fold desc="SLT LEND FUND SUCCESS TX">
/**
  * // ================== SLT LEND To Repayment TX ================ //
  * @param inputBoxes 1. SLTServiceBox 2. SLTLendBox
  * In our case, though we don't use dataInputs, we have to still insert the dataInputs
  * box for it to run successfully.
  */
case class SLTLendToRepaymentTx(
  inputBoxes: Seq[InputBox]
)(
  implicit val ctx: BlockchainContext
) extends Tx {

  def checkInputBoxes(inputBoxes: Seq[InputBox]): Boolean =
    try {
      val serviceBox: SLTServiceBox = new SLTServiceBox(inputBoxes.head)
      val lendBox: SLTLendBox = new SLTLendBox(inputBoxes(1))
      lendBox.tokens.head.getId == SLTTokens.lendTokenId &&
      serviceBox.tokens.head.getId == SLTTokens.serviceNFTId
    } catch {
      case _: Throwable => false
    }

  override def getOutBoxes: Seq[OutBox] =
    if (!checkInputBoxes(inputBoxes)) throw new IllegalArgumentException()
    else {
      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
      val wrappedSLTLendBox: SLTLendBox = new SLTLendBox(inputBoxes(1))

      val wrappedOutSLTServiceBox: SLTServiceBox =
        SLTServiceBox.mutateLendToRepaymentBox(
          new SLTServiceBox(inputBoxes.head)
        )
      val wrappedOutSLTRepaymentBox: SLTRepaymentBox = SLTRepaymentBox
        .fromFundedLendBox(wrappedSLTLendBox, ctx.getHeight.toLong)
      val wrappedOutBorrowerFundedBox: FundsToAddressBox =
        SLTLendBox.getBorrowerFundedBox(wrappedSLTLendBox)

      Seq(
        wrappedOutSLTServiceBox.getOutBox(ctx, txB),
        wrappedOutSLTRepaymentBox.getOutBox(ctx, txB),
        wrappedOutBorrowerFundedBox.getOutBox(ctx, txB)
      )
    }
}
// </editor-fold>

// <editor-fold desc="SLT REPAYMENT FUND DISTRIBUTION TX">
/**
  * // ================== SLT REPAYMENT FUND DISTRIBUTION TX ================ //
  * @param inputBoxes 1. SLTRepaymentBox
  * OutBox: 1. SLTRepaymentBox 2. FundsToLenderBox 3. FundsToExle 4. MiningFee
  * DataInputs: 1. SLTServiceBox
  * value required: 3 * Parameters.MinFee
  */
case class SLTRepaymentFundDistributionTx(
  inputBoxes: Seq[InputBox],
  override val dataInputs: Seq[InputBox]
)(implicit val ctx: BlockchainContext)
    extends Tx {

  override def getOutBoxes: Seq[OutBox] = {
    val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
    val wrappedSLTRepaymentBox: SLTRepaymentBox = new SLTRepaymentBox(
      inputBoxes.head
    )
    val wrappedSLTServiceBox: SLTServiceBox = new SLTServiceBox(dataInputs.head)

    val outSLTRepaymentBox: SLTRepaymentBox =
      SLTRepaymentDistribution.getOutRepaymentBox(wrappedSLTRepaymentBox)
    // FundsToAddressBoxes: 0. FundsToLender 1. FundsToExle
    val outFundsToAddressesBox: Seq[FundsToAddressBox] =
      SLTRepaymentDistribution.getFundsRepaidBox(
        wrappedSLTRepaymentBox,
        wrappedSLTServiceBox
      )

    Seq(
      outSLTRepaymentBox.getOutBox(ctx, txB),
      outFundsToAddressesBox.head.getOutBox(ctx, txB),
      outFundsToAddressesBox(1).getOutBox(ctx, txB)
    )
  }
}
// </editor-fold>

// <editor-fold desc="SLT REFUND TX">
/**
  * // ================== SLT LEND REFUND TX ================ //
  * @param inputBoxes 1. SLTServiceBox 2. SLTLendBox
  * OutBox: 1. SLTServiceBox 2. MiningFee
  */
case class SLTLendRefundTx(inputBoxes: Seq[InputBox])(
  implicit val ctx: BlockchainContext
) extends Tx {

  def checkInputBoxes(inputBoxes: Seq[InputBox]): Boolean =
    try {
      val serviceBox: SLTServiceBox = new SLTServiceBox(inputBoxes.head)
      val lendBox: SLTLendBox = new SLTLendBox(inputBoxes(1))
      serviceBox.tokens.head.getId == SLTTokens.serviceNFTId && lendBox.tokens.head.getId == SLTTokens.lendTokenId
    } catch {
      case _: Throwable => false
    }

  override def getOutBoxes: Seq[OutBox] =
    if (!checkInputBoxes(inputBoxes)) throw new IllegalArgumentException()
    else {
      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
      val wrappedOutSLTServiceBox: SLTServiceBox =
        SLTServiceBox.absorbLendBox(new SLTServiceBox(inputBoxes.head))

      Seq(wrappedOutSLTServiceBox.getOutBox(ctx, txB))
    }
}
// </editor-fold>
