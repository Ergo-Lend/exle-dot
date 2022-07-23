package SLTokens.txs

import SLTokens.boxes.{SLTCreateLendProxyBox, SLTFundLendProxyBox, SLTFundRepaymentProxyBox}
import commons.configs.ServiceConfig
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.{Address, BlockchainContext, BoxOperations, ErgoToken, InputBox, OutBox, ReducedTransaction}
import txs.Tx

import scala.collection.JavaConverters.{asScalaBufferConverter, seqAsJavaListConverter}

case class ProxyContractTx(inputBoxes: Seq[InputBox],
                                           outBoxes: Seq[OutBox],
                                         override val changeAddress: P2PKAddress)
                                   (implicit val ctx: BlockchainContext) extends Tx {
  override def getOutBoxes: Seq[OutBox] = outBoxes
}

case class UserFundRetriever(senderAddress: Address,
                             amountToSpend: Long,
                             tokensToSpend: Seq[ErgoToken])(implicit ctx: BlockchainContext) {
  def getInputBox: Seq[InputBox] = {
    val boxOperations: BoxOperations = BoxOperations
      .createForSender(senderAddress)
      .withAmountToSpend(amountToSpend)
      .withTokensToSpend(tokensToSpend.asJava)

    val boxOperationsTokenified: BoxOperations = if (tokensToSpend.nonEmpty)
      boxOperations.withTokensToSpend(tokensToSpend.asJava)
    else
      boxOperations

    val boxesToSpend: Seq[InputBox] = boxOperationsTokenified
      .loadTop(ctx).asScala.toSeq

    boxesToSpend
  }
}

object SLTProxyContractReducedTxRetriever {
  // Create Lend Proxy Contract
  def getCreateLendProxyContract(value: Long,
                                 borrowerPk: String,
                                 loanToken: Array[Byte],
                                 projectName: String,
                                 description: String,
                                 deadlineHeight: Long,
                                 goal: Long,
                                 interestRate: Long,
                                 repaymentHeightLength: Long
                                )(implicit ctx: BlockchainContext): ReducedTransaction = {
    // Get Input Box from UserFundRetriever
    val userInputBoxes: Seq[InputBox] = UserFundRetriever(Address.create(borrowerPk), value, Seq.empty).getInputBox

    // GetOutBoxes
    val sltCreateLendProxyBox: SLTCreateLendProxyBox = SLTCreateLendProxyBox.getBox(
      value,
      borrowerPk,
      loanToken,
      projectName,
      description,
      deadlineHeight,
      goal,
      interestRate,
      repaymentHeightLength)

    // Insert into transaction
    val userToCreateLendProxyContract: ProxyContractTx =
      ProxyContractTx(
        inputBoxes = userInputBoxes,
        outBoxes = Seq(sltCreateLendProxyBox.getOutBox(ctx, ctx.newTxBuilder())),
        changeAddress = Address.create(borrowerPk).asP2PK()
      )

    // return reduced tx
    userToCreateLendProxyContract.reduceTx
  }

  def getCreateLendProxyContract(borrowerPk: String,
                                 loanToken: Array[Byte],
                                 projectName: String,
                                 description: String,
                                 deadlineHeight: Long,
                                 goal: Long,
                                 interestRate: Long,
                                 repaymentHeightLength: Long
                                )(implicit ctx: BlockchainContext): ReducedTransaction = {
    getCreateLendProxyContract(value = ServiceConfig.serviceFee,
      borrowerPk,
      loanToken,
      projectName,
      description,
      deadlineHeight,
      goal, interestRate, repaymentHeightLength)
  }

  // Fund Lend Proxy Contract
  def getFundLendProxyContract(
    value: Long,
    tokens: Seq[ErgoToken],
    lendBoxId: String,
    lenderAddress: Address
                              )(implicit ctx: BlockchainContext): ReducedTransaction = {
    // Get InputBoxes from lender
    val userInputBoxes: Seq[InputBox] = UserFundRetriever(
      amountToSpend = value,
      tokensToSpend = tokens,
      senderAddress = lenderAddress).getInputBox

    // Get OutBox ProxyContract
    val fundLendProxyBox: OutBox = SLTFundLendProxyBox.getBox(
      boxId = lendBoxId,
      lenderAddress = lenderAddress,
      tokens = tokens,
      value = value
    ).getOutBox(ctx, ctx.newTxBuilder())

    // Create Tx
    val proxyContractTx: ProxyContractTx = new ProxyContractTx(
      inputBoxes = userInputBoxes,
      outBoxes = Seq(fundLendProxyBox),
      changeAddress = lenderAddress.asP2PK())
    // Get reduced Tx
    proxyContractTx.reduceTx
  }

  // Fund Repayment Proxy Contract
  def getFundRepaymentProxyContract(
    value: Long,
    tokens: Seq[ErgoToken],
    repaymentBoxId: String,
    funderAddress: Address
                                   )(implicit ctx: BlockchainContext): ReducedTransaction = {
    // Get InputBoxes from funder
    val userInputBoxes: Seq[InputBox] = UserFundRetriever(
      amountToSpend = value,
      tokensToSpend = tokens,
      senderAddress = funderAddress).getInputBox
    // Get OutBox ProxyContract
    val fundRepaymentProxyBox: OutBox = SLTFundRepaymentProxyBox.getBox(
      boxId = repaymentBoxId,
      value = value,
      tokens = tokens,
      fundersAddress = funderAddress
    ).getOutBox(ctx, ctx.newTxBuilder())

    // Create Tx
    val proxyContractTx: ProxyContractTx = new ProxyContractTx(
      inputBoxes = userInputBoxes,
      outBoxes = Seq(fundRepaymentProxyBox),
      changeAddress = funderAddress.asP2PK())
    // Get reduced Tx
    proxyContractTx.reduceTx
  }
}