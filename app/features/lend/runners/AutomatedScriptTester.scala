package features.lend.runners

import ergotools.{ContractUtils, TxState}
import ergotools.TxState.TxState
import ergotools.client.Client
import features.lend.LendBoxExplorer
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoContract, ErgoProver, Parameters, SecretString, SignedTransaction}

import java.util.stream.Collectors
import scala.collection.JavaConverters.seqAsJavaListConverter

object AutomatedScriptTester {
  val client = new Client()
  var explorer: LendBoxExplorer = _
  var ownerAddress: Address = _

  def main(args: Array[String]): Unit = {
    client.setClient()
    explorer = new LendBoxExplorer(client)

    setOwnerAddress(Configs.config, Configs.nodeConfig)

    val lendInitiationDetails = new LendInitiationDetails(
      deadlineHeight = 691809,
      repaymentHeight = 701809,
      walletAddress = ownerAddress.toString
    )

    val lendBoxId = "14644cdc40cf3c6df54f9c33c229cb070151c18232aca7012d2f41f2a184de53"
    val repaymentBoxId = "bf140cc7d6fa3ae697b9f026ee2cc52bce3b7ea7337fabe2d89c103a0784f667"

    val lendInitiationRunner = new LendInitiationRunner(lendInitiationDetails)
    val fundLendRunner = new FundLendRunner(
      lendBoxId = lendBoxId,
      lenderAddress = ownerAddress.toString,
      explorer = explorer)
    val fundRepaymentRunner = new FundRepaymentRunner(
      repaymentBoxId = repaymentBoxId,
      funderAddress = ownerAddress.toString,
      explorer = explorer)

    val runInitiationLend = () => {
      // Run: LendInitiation -> Get Proxy
//      lendInitiationRunner.proxyContract(client, sendErgsToContract)
//      checkTransactionComplete(proxySignedTx)
//      val lendInitiationTx = lendInitiationRunner.handleProxyMerge(client, explorer)
      // Check Proxy Mined
      // Run: LendInitiationHandler -> Save lendBoxId
      // Check Initiation Mined

      // if failed, refund
    }

    val runFundLend = () => {
      // Run: LendFund Proxy
//      fundLendRunner.proxyContract(client, sendErgsToContract)
      // Check ProxyMined
      // Run: LendFundHandler
//      fundLendRunner.handleProxyMerge(client, explorer)
      // Check Mined
      // Run: SuccessHandler
//      fundLendRunner.handleSuccess(client, explorer)
      // Check Mined -> return repayment box
    }

    val runFundRepayment = () => {
      // Run: FundRepayment Proxy
//      fundRepaymentRunner.proxyContract(client, sendErgsToContract)
//      fundRepaymentRunner.refundProxyPayment(client, explorer)
      // Check ProxyMined
      // Run: RepaymentFundHandler
//      fundRepaymentRunner.handleProxyMerge(client, explorer)
      // Check Mined
      // Run: SuccessHandler
      fundRepaymentRunner.handleSuccess(client, explorer)
      // Check Mined -> return repayment box
    }

    runInitiationLend()
    runFundLend()
    runFundRepayment()
  }

  def setOwnerAddress(config: ErgoToolConfig, nodeConfig: ErgoNodeConfig) : Unit = {
    client.getClient.execute(ctx => {
      val addressIndex = config.getParameters.get("addressIndex").toInt
      val prover = ctx.newProverBuilder().withMnemonic(
        SecretString.create(nodeConfig.getWallet.getMnemonic),
        SecretString.create("")
      ).withEip3Secret(addressIndex).build()

      ownerAddress = prover.getEip3Addresses.get(0)
    })
  }

  def getProver(ctx: BlockchainContext): ErgoProver = {
    val addressIndex = Configs.config.getParameters.get("addressIndex").toInt
    val prover = ctx.newProverBuilder().withMnemonic(
      SecretString.create(Configs.nodeConfig.getWallet.getMnemonic),
      SecretString.create("")
    ).withEip3Secret(addressIndex).build()

    prover
  }

  def sendErgsToContract(toContract: ErgoContract, amount: Long): SignedTransaction = {
    client.getClient.execute(ctx => {
      val txB = ctx.newTxBuilder()

      val totalValue = amount + Parameters.MinFee
      val coveringBoxes = ctx.getCoveringBoxesFor(ownerAddress, totalValue, null).getBoxes

      val outBox = txB.outBoxBuilder()
        .value(amount)
        .contract(toContract)
        .build()

      val tx = txB.boxesToSpend(coveringBoxes)
        .outputs(outBox)
        .fee(Parameters.MinFee)
        .sendChangeTo(ownerAddress.getErgoAddress)
        .build()

      val prover = getProver(ctx)

      val signedTx = prover.sign(tx)

      ctx.sendTransaction(signedTx)

      println(signedTx.toJson(true))

      signedTx
    })
  }

  def sendErgsToAddress(toAddress: Address, amount: Long): SignedTransaction = {
    client.getClient.execute(ctx => {
      val txB = ctx.newTxBuilder()

      val totalValue = amount + Parameters.MinFee
      val coveringBoxes = ctx.getCoveringBoxesFor(ownerAddress, totalValue, null)
      val inputBoxes = List(coveringBoxes.getBoxes.get(0)).asJava

      val outBox = txB.outBoxBuilder()
        .value(amount)
        .contract(ContractUtils.sendToPK(ctx, toAddress))
        .build()

      val tx = txB.boxesToSpend(inputBoxes)
        .outputs(outBox)
        .fee(Parameters.MinFee)
        .sendChangeTo(ownerAddress.getErgoAddress)
        .build()

      val prover = getProver(ctx)

      val signedTx = prover.sign(tx)

      ctx.sendTransaction(signedTx)

      println(signedTx.toJson(true))

      signedTx
    })
  }

  def checkTransactionComplete(signedTransaction: SignedTransaction): Boolean = {
    val checkTx = () => {
      val txState = explorer.checkTransaction(signedTransaction.getId)

      txState
    }

    var txState: TxState = checkTx()
    while (txState != TxState.Unsuccessful) {
      Thread.sleep(8000)
      txState = checkTx()

      return txState == TxState.Mined
    }

    txState == TxState.Unsuccessful
  }
}

object Configs {
  val config: ErgoToolConfig = ErgoToolConfig.load("ergo_config.json")
  val nodeConfig: ErgoNodeConfig = config.getNode
}
