package lendcore.tools.runners

import lendcore.components.ergo.TxState.TxState
import features.lend.LendBoxExplorer
import lendcore.components.ergo.{Client, ContractUtils, TxState}
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import org.ergoplatform.appkit.{Address, BlockchainContext, BoxOperations, ErgoContract, ErgoProver, ErgoToken, Parameters, SecretString, SignedTransaction}

import scala.collection.JavaConverters.seqAsJavaListConverter

object AutomatedScriptTester {
  val client = new Client()
  var explorer: LendBoxExplorer = _
  var ownerAddress: Address = _

  def main(args: Array[String]): Unit = {
    client.setClient()
    explorer = new LendBoxExplorer(client)

    setOwnerAddress(Configs.config, Configs.nodeConfig)

    val lendInitiationDetails = LendInitiationDetails(
      name = "Fund Farmers in Nigeria",
      description = "We are looking to buy more cows",
      deadlineHeight = 694140,
      repaymentHeight = 704140,
      interestRate = 10,
      goal = 1000000,
      walletAddress = "9gMg8KEYHV3oCkSeoLC42ktrWwzqaEjhvu38Yc4aBgQP9gNbJ27"
    )

    val lendBoxId = "28ecd124dcfa7f4f5d13fbe69a290464ffc22557269c79acb54966598f5975be"
    val repaymentBoxId = "bf140cc7d6fa3ae697b9f026ee2cc52bce3b7ea7337fabe2d89c103a0784f667"

    val lendInitiationRunner = LendInitiationRunner(lendInitiationDetails)
    val fundLendRunner = new FundLendRunner(
      lendBoxId = lendBoxId,
      lenderAddress = ownerAddress.toString,
      explorer = explorer)
    val fundRepaymentRunner = FundRepaymentRunner(
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
      fundLendRunner.handleSuccess(client, explorer)
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
//      fundRepaymentRunner.handleSuccess(client, explorer)
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
      val nullToken: java.util.List[ErgoToken] = List.empty[ErgoToken].asJava
      val coveringBoxes =
        BoxOperations
          .createForSender(ownerAddress)
          .withAmountToSpend(totalValue)
          .loadTop(ctx)

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
      val coveringBoxes = BoxOperations.createForSender(ownerAddress).withAmountToSpend(totalValue).loadTop(ctx)
      val inputBoxes = coveringBoxes

      val outBox = txB.outBoxBuilder()
        .value(amount)
        .contract(ContractUtils.sendToPK(toAddress))
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
      val txState = explorer.checkTransactionState(signedTransaction.getId)

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
