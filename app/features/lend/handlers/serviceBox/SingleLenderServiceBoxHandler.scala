package features.lend.handlers.serviceBox

import boxes.registers.RegisterTypes.{CollByte, NumberRegister, StringRegister}
import config.Configs
import ergotools.LendServiceTokens
import features.lend.contracts.singleLenderLendServiceBoxScript
import org.ergoplatform.appkit.{Address, BlockchainContext, BoxOperations, ConstantsBuilder, ErgoClient, ErgoClientException, ErgoContracts, ErgoId, ErgoProver, ErgoToken, ErgoWallet, InputBox, NetworkType, OutBox, Parameters, RestApiErgoClient, SecretString, SignedTransaction, UnsignedTransactionBuilder}
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}

import java.util.Optional
import java.util.stream.Collectors
import scala.collection.JavaConverters.seqAsJavaListConverter

/**
 * To create a service box, there are 4 steps
 *
 * Step 1:
 * Create NFT Box
 *
 * Step 2:
 * Create Token Box
 *
 * Step 3:
 * Merge NFT Box, and Token Box
 *
 * Step 4:
 * Verify service Box
 */
object SingleLenderServiceBoxHandler {
  def main(args: Array[String]): Unit = {

    val configFileName = "ergo_config.json"
    val conf: ErgoToolConfig = ErgoToolConfig.load(configFileName)
    val nodeConf: ErgoNodeConfig = conf.getNode

    val client: ErgoClient = RestApiErgoClient.create(Configs.nodeUrl, Configs.networkType, "hello", Configs.explorerUrl)
    System.out.println(s"Network Type is: ${Configs.networkType}")

    val txJson: String = client.execute((ctx: BlockchainContext) => {
      val runTx = "createNFT"

      val signedTx: SignedTransaction = runTx match {
        case "createNFT" => createNFTBox(ctx, conf, nodeConf)
        case "serviceBox" => mergeCreateServiceBox(ctx, conf, nodeConf)
//        case "verify" => verifyServiceBox(ctx, conf, nodeConf)
      }

      ctx.sendTransaction(signedTx)
      val jsonVal = signedTx.toJson(true)
      System.out.println(jsonVal)
      jsonVal
    })
  }

  def createNFTBox(ctx: BlockchainContext, config: ErgoToolConfig, nodeConfig: ErgoNodeConfig): SignedTransaction = {

    // Token Info
    val nftName = "LendServiceNFT"
    val nftDesc = "LendServiceNFT: Grassroots Lending"
    val tokenName = "ErgoLendTest"
    val tokenDesc = "Who's your papa: K daddy in the house yo"

    val addressIndex: Int = config.getParameters.get("addressIndex").toInt
    val prover: ErgoProver = ctx.newProverBuilder().withMnemonic(
      SecretString.create(nodeConfig.getWallet.getMnemonic),
      SecretString.create("")
    )
      .withEip3Secret(addressIndex)
      .build()

    val ownerAddress: Address = prover.getEip3Addresses.get(0)
    val amountToSend: Long = 1000L
    val totalToSpend: Long = amountToSend + Parameters.MinFee
    val boxesToSpend: java.util.List[InputBox] = ctx.getCoveringBoxesFor(ownerAddress, totalToSpend).getBoxes
    val spendingBoxes = ctx.getUnspentBoxesFor(ownerAddress, 0, 100)

    val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()

    val token = new ErgoToken(boxesToSpend.get(0).getId, 1L)
    val tokensBox: OutBox = txB.outBoxBuilder()
      .value(amountToSend)
      .mintToken(token, tokenName, tokenDesc, 0)
      .contract(ErgoContracts.sendToPK(ctx, ownerAddress))
      .build()

    val tx = txB
      .boxesToSpend(boxesToSpend)
      .outputs(tokensBox)
      .fee(Parameters.MinFee)
      .sendChangeTo(ownerAddress.getErgoAddress)
      .build()

    val signed: SignedTransaction = prover.sign(tx)

    signed
  }

  def mergeCreateServiceBox(ctx: BlockchainContext, config: ErgoToolConfig, nodeConfig: ErgoNodeConfig): SignedTransaction = {
    val serviceNftBoxId: String = config.getParameters.get("serviceNFTBoxId")
    val tokensBoxId: String = config.getParameters.get("tokensBoxId")

    val addressIndex: Int = config.getParameters.get("addressIndex").toInt
    val ownerAddress: Address = Address.createEip3Address(
      0,
      Configs.networkType,
      SecretString.create(nodeConfig.getWallet.getMnemonic),
      SecretString.create(""))
    val ownerAddressString: String = ownerAddress.toString

    val ergoLendPubKeyRegister: StringRegister = new StringRegister(ownerAddressString)
    val profitSharingPercentageRegister: NumberRegister = new NumberRegister(8)

    val prover: ErgoProver = ctx.newProverBuilder().withMnemonic(
      SecretString.create(nodeConfig.getWallet.getMnemonic),
      SecretString.create(nodeConfig.getWallet.getPassword)
    )
      .withEip3Secret(addressIndex)
      .build()


    val spendingBoxes = ctx.getUnspentBoxesFor(ownerAddress, 0, 100)

    val spendingBoxesWithServiceNft: java.util.List[InputBox] = spendingBoxes
      .stream()
      .filter(_.getId == ErgoId.create(serviceNftBoxId))
      .collect(Collectors.toList())

    val spendingBoxesWithTokensBoxes: java.util.List[InputBox] = spendingBoxes
      .stream()
      .filter(_.getId == ErgoId.create(tokensBoxId))
      .collect(Collectors.toList())

    val inputBoxes = List(spendingBoxesWithServiceNft.get(0), spendingBoxesWithTokensBoxes.get(0)).asJava

    val amountToSend: Long = Parameters.OneErg - Parameters.MinFee

    val txB = ctx.newTxBuilder()

    val serviceBoxContract = ctx.compileContract(ConstantsBuilder.create()
      .item("ownerPk", ownerAddress.getPublicKey)
      .item("serviceNFT", spendingBoxesWithServiceNft.get(0).getTokens.get(0).getId)
      .item("serviceToken", spendingBoxesWithTokensBoxes.get(0).getTokens.get(0).getId)
      .build(), singleLenderLendServiceBoxScript)

    val serviceBox = txB.outBoxBuilder
      .value(amountToSend)
      .contract(serviceBoxContract)
      .tokens(
        spendingBoxesWithServiceNft.get(0).getTokens.get(0),
        spendingBoxesWithTokensBoxes.get(0).getTokens.get(0)
      )
      .registers(
        ergoLendPubKeyRegister.toRegister,
        profitSharingPercentageRegister.toRegister
      )
      .build()

    val tx = txB.boxesToSpend(inputBoxes)
      .outputs(serviceBox)
      .fee(Parameters.MinFee)
      .sendChangeTo(prover.getP2PKAddress)
      .build()

    val signed = prover.sign(tx)

    signed
  }

//  def verifyServiceBox(ctx: BlockchainContext, config: ErgoToolConfig, nodeConfig: ErgoNodeConfig): SignedTransaction = {}
}
