package features.lend.handlers.serviceBox

import boxes.registers.RegisterTypes.{CollByte, NumberRegister, StringRegister}
import config.Configs
import ergotools.LendServiceTokens
import features.lend.boxes.registers.{CreationInfoRegister, ProfitSharingRegister, ServiceBoxInfoRegister, SingleAddressRegister}
import features.lend.contracts.{singleLenderLendServiceBoxScript}
import org.ergoplatform.appkit
import org.ergoplatform.appkit.{Address, BlockchainContext, ConstantsBuilder, ErgoClient, ErgoContracts, ErgoId, ErgoProver, ErgoToken, InputBox, OutBox, Parameters, RestApiErgoClient, SecretString, SignedTransaction, UnsignedTransactionBuilder}
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}

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
      val runTx = "merge"

      System.out.println(s"Running ${runTx} tx")

      val signedTx: SignedTransaction = runTx match {
        case "create" => createNFTBox(ctx, conf, nodeConf)
        case "merge" => mergeCreateServiceBox(ctx, conf, nodeConf)
        case "redeem" => redeemServiceBox(ctx, conf, nodeConf)

        // Tests
        case "test" => testScript(ctx, conf, nodeConf)
//        case "verify" => verifyServiceBox(ctx, conf, nodeConf)
      }

      ctx.sendTransaction(signedTx)
      val jsonVal = signedTx.toJson(true)
      System.out.println(jsonVal)
      jsonVal
    })

    System.out.println("Completed Transaction")
  }

  def testScript(ctx: BlockchainContext, config: ErgoToolConfig, nodeConfig: ErgoNodeConfig): SignedTransaction = {

    val addressIndex = config.getParameters.get("addressIndex").toInt

    val prover = ctx.newProverBuilder().withMnemonic(
      SecretString.create(nodeConfig.getWallet.getMnemonic),
      SecretString.create("")
    ).withEip3Secret(addressIndex).build()

    val ownerAddress = prover.getEip3Addresses.get(0)

    val txB = ctx.newTxBuilder()

    val contract = ctx.compileContract(ConstantsBuilder.create()
      .item("ownerPk", ownerAddress.getPublicKey)
      .item("serviceNFT", LendServiceTokens.nft.getBytes)
      .item("lendToken", LendServiceTokens.lendToken.getBytes)
      .item("repaymentToken", LendServiceTokens.repaymentToken.getBytes)
      .build(), singleLenderLendServiceBoxScript)

    val serviceBoxAddressString = Configs.addressEncoder.fromProposition(contract.getErgoTree).get.toString
    val serviceBoxAddressString2 = "3ehvgA25dg5PNkqGwAuTXojtd6nkNmjkxTiT5zMGG2xG74pVQNt8ie9a8VLmxxovJvxgt2iah6Jz24UL5zjvZPJnVpHU3vB5Q88G15Sr3DVy413vrgNbE83h7HCfZTtHwwoV7uv4Rirhe2WkC1wue9rYhFSejSeicu4DZjqAhqQj6YxEpsUAw8tjLPFCbqWtHEBJQrCMEXFc5MyZPiKsDRSnEX97tqU8SRBFB5yo5RRQsfYKtaEn4X41jmVEZuTab2vy9Vgo9SKvi2JmcirYQ1kpC1R2MvsARAm9AF1PhwsNZ4Q8DTGWDAnfmLYvMu23ThBtantWnmLgRhavm1VPkyAEpaqPha4j2f53NrKMJ9jrUmiVAExi84dVb3kmWRW7UUxKvjCUtH6GWTAHACc8NvgBzPA96Tewesf1h7PL8VWhJXHAoqxNTbJiKFgusZJXADZurPaTzvdW3ch62V8i4TVQmPwj6SSLRQG6R9vibzm7EVWP46eVLo4rzZUz8njuLLTyBRzthF1eK1m7GKzM42eeYMEsRaCwwcuqFN9K6qo21uP6pSm3yQdpJEoSzdLBeqbn6A8CJGpujNH"
    val serviceBoxAddress = Address.create(serviceBoxAddressString2)
    val serviceBox = ctx.getUnspentBoxesFor(serviceBoxAddress, 0, 100).get(0)

    val unspentBoxes = ctx.getUnspentBoxesFor(ownerAddress, 0, 100)


//    val tokenBoxes: java.util.List[InputBox] = unspentBoxes
//      .stream()
//      .filter(!_.getTokens.isEmpty)
//      .collect(Collectors.toList())
//
//    val serviceBoxList: java.util.List[InputBox] = tokenBoxes
//      .stream()
//      .filter(_.getTokens.get(0).getId == LendServiceTokens.lendToken)
//      .collect(Collectors.toList())
//    val lendBox = serviceBoxList.get(0)

    val serviceBoxLendToken = new ErgoToken(
      serviceBox.getTokens.get(1).getId,
      serviceBox.getTokens.get(1).getValue)

    val serviceBoxRepaymentToken = new ErgoToken(
      serviceBox.getTokens.get(1).getId,
      serviceBox.getTokens.get(1).getValue - 1)

    val lendBoxLendToken = new ErgoToken(
      serviceBox.getTokens.get(1).getId,
      1
    )

    val repaymentBoxRepaymentToken = new ErgoToken(
      serviceBox.getTokens.get(2).getId,
      1
    )

    val outServiceBox = txB.outBoxBuilder()
      .value(Configs.minBoxErg)
      .tokens(
        serviceBox.getTokens.get(0),
        serviceBoxLendToken,
        serviceBoxRepaymentToken
      )
      .registers(
        serviceBox.getRegisters.get(0),
        serviceBox.getRegisters.get(1)
      )
      .contract(ErgoContracts.sendToPK(ctx, ownerAddress))
      .build()

    val outLendBox = txB.outBoxBuilder()
      .value(Configs.minBoxErg)
      .tokens(
        lendBoxLendToken
      )
      .contract(ErgoContracts.sendToPK(ctx, ownerAddress))
      .build()

    val outRepaymentBox = txB.outBoxBuilder()
      .value(Configs.minBoxErg)
      .tokens(
        repaymentBoxRepaymentToken
      )
      .contract(ErgoContracts.sendToPK(ctx, ownerAddress))
      .build()

    val coveringBoxes = ctx.getCoveringBoxesFor(ownerAddress, Parameters.MinFee ).getBoxes.get(0)

    val inputBoxes = List(serviceBox, coveringBoxes).asJava

    val tx = txB.boxesToSpend(inputBoxes)
      .outputs(outServiceBox)
      .fee(Parameters.MinFee)
      .sendChangeTo(ownerAddress.getErgoAddress)
      .build()

    val signedTx = prover.sign(tx)

    signedTx
  }

  def createNFTBox(ctx: BlockchainContext, config: ErgoToolConfig, nodeConfig: ErgoNodeConfig): SignedTransaction = {

    // Token Info
    val nftName = "LendTest"
    val nftDesc = "LendTestNFT: Grassroots Lending"
    val lendTokenName = "LendTokenTest"
    val lendTokenDesc = "LendTokenTest"
    val repaymentTokenName = "RepaymentTokenTest"
    val repaymentTokenDesc = "RepaymentTokenTest"

    // service
    // lend
    // repayment
    val tokenCreate = "repayment"

    val addressIndex: Int = config.getParameters.get("addressIndex").toInt

    val prover: ErgoProver = ctx.newProverBuilder().withMnemonic(
      SecretString.create(nodeConfig.getWallet.getMnemonic),
      SecretString.create("")
    )
      .withEip3Secret(addressIndex)
      .build()

    val ownerAddress: Address = prover.getEip3Addresses.get(0)

    val directBox = ctx.getUnspentBoxesFor(ownerAddress, 0, 100)
    System.out.println("boxId: " + directBox.get(0).getId)

    val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()

    val token = tokenCreate match {
      case "service" => new ErgoToken(directBox.get(0).getId, 1L)
      case "lend" => new ErgoToken(directBox.get(0).getId, 1000000000L)
      case "repayment" => new ErgoToken(directBox.get(0).getId, 1000000000L)
    }

    val tokenBox: OutBox = tokenCreate match {
      case "service" => {
        txB.outBoxBuilder()
          .value(Configs.minBoxErg)
          .mintToken(token, nftName, nftDesc, 0)
          .contract(ErgoContracts.sendToPK(ctx, ownerAddress))
          .build()
      }

      case "lend" => {
        txB.outBoxBuilder()
          .value(Configs.minBoxErg)
          .mintToken(token, lendTokenName, lendTokenDesc, 0)
          .contract(ErgoContracts.sendToPK(ctx, ownerAddress))
          .build()
      }

      case "repayment" => {
        txB.outBoxBuilder()
          .value(Configs.minBoxErg)
          .mintToken(token, repaymentTokenName, repaymentTokenDesc, 0)
          .contract(ErgoContracts.sendToPK(ctx, ownerAddress))
          .build()
      }
    }
    val inputBoxes = List(directBox.get(0)).asJava

    val tx = txB
      .boxesToSpend(inputBoxes)
      .outputs(tokenBox)
      .fee(Parameters.MinFee)
      .sendChangeTo(ownerAddress.getErgoAddress)
      .build()

    val signed: SignedTransaction = prover.sign(tx)

    signed
  }

  def mergeCreateServiceBox(ctx: BlockchainContext, config: ErgoToolConfig, nodeConfig: ErgoNodeConfig): SignedTransaction = {
    val serviceNftBoxId: String = LendServiceTokens.nftString
    val lendTokenId: String = LendServiceTokens.lendTokenString
    val repaymentTokenId: String = LendServiceTokens.repaymentTokenString

    val addressIndex: Int = config.getParameters.get("addressIndex").toInt
    val ownerAddress: Address = Address.createEip3Address(
      0,
      Configs.networkType,
      SecretString.create(nodeConfig.getWallet.getMnemonic),
      SecretString.create(""))
    val ownerAddressString: String = ownerAddress.toString

    val creationInfo: CreationInfoRegister = new CreationInfoRegister(creationHeight = ctx.getHeight)
    val serviceInfo: ServiceBoxInfoRegister = new ServiceBoxInfoRegister(name = "ErgoLend", description = "A Lending Platform on Ergo")
    val boxInfo: StringRegister = new StringRegister("SingleLenderServiceBox")
    val ergoLendPubKeyRegister: SingleAddressRegister = new SingleAddressRegister(ownerAddressString)
    val profitSharingPercentageRegister: ProfitSharingRegister = new ProfitSharingRegister(profitSharingPercentage = 18L)

    val prover: ErgoProver = ctx.newProverBuilder().withMnemonic(
      SecretString.create(nodeConfig.getWallet.getMnemonic),
      SecretString.create(nodeConfig.getWallet.getPassword)
    )
      .withEip3Secret(addressIndex)
      .build()


    val spendingBoxes = ctx.getUnspentBoxesFor(ownerAddress, 0, 100)

    val spendingBoxesWithTokens: java.util.List[InputBox] = spendingBoxes.stream().filter(!_.getTokens.isEmpty).collect(Collectors.toList())

    val spendingBoxesWithServiceNft: java.util.List[InputBox] = spendingBoxesWithTokens
      .stream()
      .filter(_.getTokens.get(0).getId == ErgoId.create(serviceNftBoxId))
      .collect(Collectors.toList())

    val spendingBoxesWithLendTokensBoxes: java.util.List[InputBox] = spendingBoxesWithTokens
      .stream()
      .filter(_.getTokens.get(0).getId == ErgoId.create(lendTokenId))
      .collect(Collectors.toList())

    val spendingBoxesWithRepaymentTokensBoxes: java.util.List[InputBox] = spendingBoxesWithTokens
      .stream()
      .filter(_.getTokens.get(0).getId == ErgoId.create(repaymentTokenId))
      .collect(Collectors.toList())

//    val inputBoxes = List(
//      spendingBoxesWithServiceNft.get(0),
//      spendingBoxesWithLendTokensBoxes.get(0),
//      spendingBoxesWithRepaymentTokensBoxes.get(0)).asJava

    val amountToSend: Long = Configs.minBoxErg + Parameters.MinFee

    val txB = ctx.newTxBuilder()

    val serviceBoxContract = ctx.compileContract(ConstantsBuilder.create()
      .item("ownerPk", ownerAddress.getPublicKey)
      .item("serviceNFT", LendServiceTokens.nft.getBytes)
      .item("serviceLendToken", LendServiceTokens.lendToken.getBytes)
      .item("serviceRepaymentToken", LendServiceTokens.repaymentToken.getBytes)
      .build(), singleLenderLendServiceBoxScript)

    val serviceBox = txB.outBoxBuilder
      .value(amountToSend)
      .contract(serviceBoxContract)
      .tokens(
        spendingBoxesWithServiceNft.get(0).getTokens.get(0),
        spendingBoxesWithServiceNft.get(0).getTokens.get(1),
        spendingBoxesWithServiceNft.get(0).getTokens.get(2),
      )
      .registers(
        creationInfo.toRegister,
        serviceInfo.toRegister,
        boxInfo.toRegister,
        ergoLendPubKeyRegister.toRegister,
        profitSharingPercentageRegister.toRegister
      )
      .build()

    val coveringBoxes = ctx.getCoveringBoxesFor(ownerAddress, Parameters.MinFee ).getBoxes.get(0)
    val inputBoxes = List(spendingBoxesWithServiceNft.get(0), coveringBoxes).asJava

    val tx = txB.boxesToSpend(inputBoxes)
      .outputs(serviceBox)
      .fee(Parameters.MinFee)
      .sendChangeTo(ownerAddress.getErgoAddress)
      .build()

    val signed = prover.sign(tx)

    signed
  }

  def redeemServiceBox(ctx: BlockchainContext, config: ErgoToolConfig, node: ErgoNodeConfig): SignedTransaction = {
    val serviceBoxAddress = Address.create("mXEzPNaBeFwXReCZJB6X12wEeLrtLN1aPd1bdrT1XuwfRdyNL2at2dRdnZA1hnRXqhL3u8SiA18hMfbQ3jgUnzoBqnGWfYNYsU8FME3NhGnWW1e961j1kKop7EJATXgt1Jpg5D2ELe6ZKB3eyyPJVXYVBGmMbzfTfUhdnbQDLQJSYAPwc65xbPiWhoJUP5PM6qJjvCKXUjAEmUxr7bTQkvCKjWS1sng2Dc9HJDp4RawsWFPeB8PAVTWtPV98iumEN64RwnzXfH5HzueBFe3ngJmsp9NziH7EFCVk8xXLf21Ay545AZMtMUw8txNZwp5Au5UhquJYHPfrA7WJ4ZKT54tWe8bRw2BwLBNmMcNqfFZEwsthRCuphjASV8WfTuhC34E3bsT1pWpYEPRciEKKA2otka1TjXWn7rbVVReufcc9AY3jaN5P5WsaWrrDT9HRtkNc3h4gNrMTi7QTnNV137zGjqKvRvHkRMttVctHjFGt6DZ8ZkUyVfScVJq")
    val spendingBoxes = ctx.getUnspentBoxesFor(serviceBoxAddress, 0, 100)

    val ownerAddress = Address.createEip3Address(
      0,
      Configs.networkType,
      SecretString.create(node.getWallet.getMnemonic),
      SecretString.create(""))

    val prover = ctx.newProverBuilder().withMnemonic(
      SecretString.create(node.getWallet.getMnemonic),
      SecretString.create(node.getWallet.getPassword)
    ).withEip3Secret(config.getParameters.get("addressIndex").toInt).build()

    val txB = ctx.newTxBuilder()
    val serviceBox = spendingBoxes.get(0)

    val coveringBoxes = ctx.getCoveringBoxesFor(ownerAddress, Parameters.MinFee ).getBoxes.get(0)
    val inputBoxes = List(serviceBox, coveringBoxes).asJava

    val serviceBoxRedeemed = txB.outBoxBuilder
      .value(serviceBox.getValue)
      .contract(ErgoContracts.sendToPK(ctx, ownerAddress))
      .tokens(
        serviceBox.getTokens.get(0),
        serviceBox.getTokens.get(1),
        serviceBox.getTokens.get(2)
      )
      .build()

    val tx = txB.boxesToSpend(inputBoxes)
      .outputs(serviceBoxRedeemed)
      .fee(Parameters.MinFee)
      .sendChangeTo(ownerAddress.getErgoAddress)
      .build()

    val signed = prover.sign(tx)
    signed
  }

//  def verifyServiceBox(ctx: BlockchainContext, config: ErgoToolConfig, nodeConfig: ErgoNodeConfig): SignedTransaction = {}
}
