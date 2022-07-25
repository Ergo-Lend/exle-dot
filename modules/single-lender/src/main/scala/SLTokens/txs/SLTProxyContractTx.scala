package SLTokens.txs

import SLTokens.boxes.{
  SLTCreateLendProxyBox,
  SLTFundLendProxyBox,
  SLTFundRepaymentProxyBox
}
import commons.configs.ServiceConfig
import commons.ergo.ErgCommons
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  BoxOperations,
  ErgoToken,
  InputBox,
  OutBox,
  ReducedTransaction
}
import txs.Tx

import scala.collection.JavaConverters.{
  asScalaBufferConverter,
  seqAsJavaListConverter
}

case class ProxyContractTx(
  inputBoxes: Seq[InputBox],
  outBoxes: Seq[OutBox],
  override val changeAddress: P2PKAddress
)(implicit val ctx: BlockchainContext)
    extends Tx {
  override def getOutBoxes: Seq[OutBox] = outBoxes
}

case class UserFundRetriever(
  senderAddress: Address,
  amountToSpend: Long,
  tokensToSpend: Seq[ErgoToken]
)(implicit ctx: BlockchainContext) {

  def getInputBox: Seq[InputBox] = {
    val boxOperations: BoxOperations = BoxOperations
      .createForSender(senderAddress)
      .withAmountToSpend(amountToSpend)
      .withTokensToSpend(tokensToSpend.asJava)

    val boxOperationsTokenified: BoxOperations =
      if (tokensToSpend.nonEmpty)
        boxOperations.withTokensToSpend(tokensToSpend.asJava)
      else
        boxOperations

    val boxesToSpend: Seq[InputBox] = boxOperationsTokenified
      .loadTop(ctx)
      .asScala
      .toSeq

    boxesToSpend
  }
}

/**
  * Returns (Address, ReducedTransaction)
  */
object SLTProxyContractReducedTxRetriever {

  // Create Lend Proxy Contract
  def getCreateLendProxyContract(
    value: Long,
    borrowerPk: String,
    loanToken: Array[Byte],
    projectName: String,
    description: String,
    deadlineHeight: Long,
    goal: Long,
    interestRate: Long,
    repaymentHeightLength: Long
  )(implicit ctx: BlockchainContext): (Address, ReducedTransaction) = {
    // Get Input Box from UserFundRetriever
    val userInputBoxes: Seq[InputBox] = UserFundRetriever(
      Address.create(borrowerPk),
      value,
      Seq.empty
    ).getInputBox

    // GetOutBoxes
    val sltCreateLendProxyBox: SLTCreateLendProxyBox =
      SLTCreateLendProxyBox.getBox(
        borrowerPk = borrowerPk,
        loanToken = loanToken,
        projectName = projectName,
        description = description,
        deadlineHeight = deadlineHeight,
        goal = goal,
        interestRate = interestRate,
        repaymentHeightLength = repaymentHeightLength,
        value = value
      )

    // Insert into transaction
    val userToCreateLendProxyContract: ProxyContractTx =
      ProxyContractTx(
        inputBoxes = userInputBoxes,
        outBoxes =
          Seq(sltCreateLendProxyBox.getOutBox(ctx, ctx.newTxBuilder())),
        changeAddress = Address.create(borrowerPk).asP2PK()
      )

    // return reduced tx
    (
      sltCreateLendProxyBox.getContract(ctx).getAddress,
      userToCreateLendProxyContract.reduceTx
    )
  }

  def getCreateLendProxyContract(
    borrowerPk: String,
    loanToken: Array[Byte],
    projectName: String,
    description: String,
    deadlineHeight: Long,
    goal: Long,
    interestRate: Long,
    repaymentHeightLength: Long
  )(implicit ctx: BlockchainContext): (Address, ReducedTransaction) =
    getCreateLendProxyContract(
      value = ServiceConfig.serviceFee + ErgCommons.MinMinerFee * 2,
      borrowerPk,
      loanToken,
      projectName,
      description,
      deadlineHeight,
      goal,
      interestRate,
      repaymentHeightLength
    )

  // Fund Lend Proxy Contract
  def getFundLendProxyContract(
    value: Long,
    tokens: Seq[ErgoToken],
    lendBoxId: Array[Byte],
    lenderAddress: Address
  )(implicit ctx: BlockchainContext): (Address, ReducedTransaction) = {
    // Get InputBoxes from lender
    val userInputBoxes: Seq[InputBox] = UserFundRetriever(
      amountToSpend = value,
      tokensToSpend = tokens,
      senderAddress = lenderAddress
    ).getInputBox

    // Get OutBox ProxyContract
    val fundLendProxyBox: SLTFundLendProxyBox = SLTFundLendProxyBox
      .getBox(
        boxId = lendBoxId,
        lenderAddress = lenderAddress,
        tokens = tokens,
        value = value
      )

    // Create Tx
    val proxyContractTx: ProxyContractTx = new ProxyContractTx(
      inputBoxes = userInputBoxes,
      outBoxes = Seq(fundLendProxyBox.getOutBox(ctx, ctx.newTxBuilder())),
      changeAddress = lenderAddress.asP2PK()
    )
    // Get reduced Tx
    (fundLendProxyBox.getContract(ctx).getAddress, proxyContractTx.reduceTx)
  }

  // Fund Repayment Proxy Contract
  def getFundRepaymentProxyContract(
    value: Long,
    tokens: Seq[ErgoToken],
    repaymentBoxId: Array[Byte],
    funderAddress: Address
  )(implicit ctx: BlockchainContext): (Address, ReducedTransaction) = {
    // Get InputBoxes from funder
    val userInputBoxes: Seq[InputBox] = UserFundRetriever(
      amountToSpend = value,
      tokensToSpend = tokens,
      senderAddress = funderAddress
    ).getInputBox
    // Get OutBox ProxyContract
    val fundRepaymentProxyBox: SLTFundRepaymentProxyBox =
      SLTFundRepaymentProxyBox
        .getBox(
          boxId = repaymentBoxId,
          currentBoxValue = value,
          tokens = tokens,
          fundersAddress = funderAddress
        )

    // Create Tx
    val proxyContractTx: ProxyContractTx = ProxyContractTx(
      inputBoxes = userInputBoxes,
      outBoxes = Seq(fundRepaymentProxyBox.getOutBox(ctx, ctx.newTxBuilder())),
      changeAddress = funderAddress.asP2PK()
    )
    // Get reduced Tx
    (
      fundRepaymentProxyBox.getContract(ctx).getAddress,
      proxyContractTx.reduceTx
    )
  }
}
