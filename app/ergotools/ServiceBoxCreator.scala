package ergotools

import org.ergoplatform.appkit._
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}

import java.util.Optional

/**
 * ServiceBoxCreator
 *
 * Creates service box with NFT
 * creates box with tokens
 */
object ServiceBoxCreator {

  def sendTx(configFileName: String,
             serviceNFTTokenName: String,
             serviceNFTDescription: String,
             tokenName: String,
             tokenDescription: String): String = {
    val conf: ErgoToolConfig = ErgoToolConfig.load(configFileName)
    val nodeConf: ErgoNodeConfig = conf.getNode
    val explorerUrl: String = RestApiErgoClient.getDefaultExplorerUrl(NetworkType.TESTNET)

    val addressIndex: Int = conf.getParameters.get("addressIndex").toInt

    val ergoClient: ErgoClient = RestApiErgoClient.create(nodeConf, explorerUrl)

    val txJson: String = ergoClient.execute((ctx: BlockchainContext) => {
      val amountToSend: Long = Parameters.OneErg
      val totalToSpend: Long = amountToSend + Parameters.MinFee

      val mnemonicString = nodeConf.getWallet.getMnemonic.toCharArray()
      val passwordString = nodeConf.getWallet.getPassword.toCharArray()
      val mnemonic = Mnemonic.create(mnemonicString, passwordString)

      val prover = BoxOperations.createProver(ctx, mnemonic)
      val sender = prover.getAddress
      val unspent = ctx.getUnspentBoxesFor(sender, 0, 100)
      val boxesToSpend = BoxOperations.selectTop(unspent, totalToSpend)

      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()

      val serviceNft = new ErgoToken(boxesToSpend.get(0).getId, 1)
      val serviceNftBox: OutBox = txB.outBoxBuilder()
        .value(amountToSend)
        .mintToken(serviceNft, serviceNFTTokenName, serviceNFTDescription, 0)
        .contract(ErgoContracts.sendToPK(ctx, prover.getAddress))
        .build()

      val token = new ErgoToken(boxesToSpend.get(1).getId, 1000000000)
      val tokensBox: OutBox = txB.outBoxBuilder()
        .value(amountToSend)
        .mintToken(token, tokenName, tokenDescription, 0)
        .contract(ErgoContracts.sendToPK(ctx, prover.getAddress))
        .build()

      val tx = txB
        .boxesToSpend(boxesToSpend).outputs(serviceNftBox, tokensBox)
        .fee(Parameters.MinFee)
        .sendChangeTo(prover.getP2PKAddress)
        .build()

      val signed: SignedTransaction = prover.sign(tx)

      val txId: String = ctx.sendTransaction(signed)

      signed.toJson(true)
    })

    txJson
  }

  def main(args: Array[String]): Unit = {
    val serviceNFTName = "SingleLenderLendServiceNFT"
    val serviceNFTDescription = "SingleLenderLendServiceNFT: The service box that manages SingleLenderLendBox"
    val tokenName = "SingleLenderBoxToken"
    val tokenDescription = "SingleLenderBoxToken: LendingBoxes that allows one lender to completely fund a lending box"
    val txJson: String = sendTx("ergo_config.json", serviceNFTName, serviceNFTDescription, tokenName, tokenDescription)
    System.out.println(txJson)
  }
}

/**
 * ServiceBoxMerger
 *
 * Merges both serviceNFT and tokens into one box,
 * and place the service box contract on the box.
 */
object ServiceBoxMerger {
  def main(args: Array[String]): Unit = {
    val txJson: String = sendTx("ergo_config.json")
    System.out.println(txJson)
  }

  def sendTx(configFileName: String) : String = {
    val conf: ErgoToolConfig = ErgoToolConfig.load(configFileName)
    val nodeConf: ErgoNodeConfig = conf.getNode
    val explorerUrl: String = RestApiErgoClient.getDefaultExplorerUrl(NetworkType.TESTNET)

    val addressIndex: Int = conf.getParameters.get("addressIndex").toInt

    val ergoClient: ErgoClient = RestApiErgoClient.create(nodeConf, explorerUrl)

    val txJson: String = ergoClient.execute((ctx: BlockchainContext) => {
      val prover: ErgoProver = ctx.newProverBuilder.withMnemonic(
        SecretString.create(nodeConf.getWallet.getMnemonic),
        SecretString.create(nodeConf.getWallet.getPassword)
      )
        .withEip3Secret(addressIndex)
        .build()

      val wallet: ErgoWallet = ctx.getWallet
      val amountToSend: Long = Parameters.OneErg
      val totalToSpend: Long = amountToSend + Parameters.MinFee
      val boxes: Optional[java.util.List[InputBox]] = wallet.getUnspentBoxes(totalToSpend)
      if (!boxes.isPresent)
        throw new ErgoClientException(s"Not enough coins", null)

      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()

      val unspent = ctx.getUnspentBoxesFor(prover.getAddress, 0, 10)
      val boxesToSpend = BoxOperations.selectTop(unspent, amountToSend)
      val serviceNft = new ErgoToken(boxesToSpend.get(0).getId, 1)
      val serviceBox: OutBox = txB.outBoxBuilder()
        .value(amountToSend)
        .contract(ErgoContracts.sendToPK(ctx, prover.getAddress))
        .build()

      val tx = txB
        .boxesToSpend(boxes.get)
        .outputs(serviceBox)
        .fee(Parameters.MinFee)
        .sendChangeTo(prover.getP2PKAddress)
        .build()

      val signed: SignedTransaction = prover.sign(tx)

      val txId: String = ctx.sendTransaction(signed)

      signed.toJson(true)
    })

    txJson
  }
}
