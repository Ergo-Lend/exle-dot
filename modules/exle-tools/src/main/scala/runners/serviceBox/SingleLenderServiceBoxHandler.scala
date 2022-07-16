package runners.serviceBox

import SLErgs.LendServiceTokens
import SLErgs.contracts.SLEServiceBoxContract
import commons.registers.{
  CreationInfoRegister,
  ProfitSharingRegister,
  ServiceBoxInfoRegister,
  SingleAddressRegister
}
import commons.boxes.registers.RegisterTypes.StringRegister
import commons.configs.{NodeConfig, ServiceConfig}
import commons.configs.NodeConfig.SystemNodeConfig
import commons.ergo.{ContractUtils, ErgCommons}
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  BoxOperations,
  ErgoClient,
  ErgoId,
  ErgoProver,
  ErgoToken,
  InputBox,
  OutBox,
  Parameters,
  RestApiErgoClient,
  SecretString,
  SignedTransaction,
  UnsignedTransactionBuilder
}
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
object SLEServiceBoxHandler {

  def main(args: Array[String]): Unit = {

    val configFileName = "ergo_config.json"
    val conf: ErgoToolConfig = ErgoToolConfig.load(configFileName)
    val nodeConf: ErgoNodeConfig = conf.getNode

    val client: ErgoClient = RestApiErgoClient.create(
      SystemNodeConfig.nodeUrl,
      NodeConfig.networkType,
      "hello",
      SystemNodeConfig.explorerUrl
    )
    System.out.println(s"Network Type is: ${NodeConfig.networkType}")

    val txJson: String = client.execute { (ctx: BlockchainContext) =>
      val runTx = "merge"

      System.out.println(s"Running $runTx tx")

      val signedTx: SignedTransaction = runTx match {
        case "create" => createNFTBox(ctx, conf, nodeConf)
        case "merge"  => mergeCreateServiceBox(ctx, conf, nodeConf)
        case "redeem" => redeemServiceBox(ctx, conf, nodeConf)

        // Tests
//        case "verify" => verifyServiceBox(ctx, conf, nodeConf)
      }

      ctx.sendTransaction(signedTx)
      val jsonVal = signedTx.toJson(true)
      System.out.println(jsonVal)
      jsonVal
    }

    System.out.println("Completed Transaction")
  }

  def createNFTBox(
    ctx: BlockchainContext,
    config: ErgoToolConfig,
    nodeConfig: ErgoNodeConfig
  ): SignedTransaction = {

    // Token Info
    val nftName = "ErgoLend v0.1.1"
    val nftDesc = "v0.1.1: SingleLender Erg P2P Lend Box Token"
    val lendTokenName = "ErgoLend's SingleLender Erg P2P Lend Token"
    val lendTokenDesc =
      "This token identifies a box to be a lend box, or an equivalent to a loan box, where lender can fund it fully to ensure funds are transferred to the borrower's wallet."
    val repaymentTokenName = "SingleLender Erg P2P Repayment Box Token"
    val repaymentTokenDesc =
      "This token identifies a box to be a repayment box, where borrowers or funders can fund this box in steps or fully, and it will ensure that funds are repaid back to lender."

    // service
    // lend
    // repayment
    val tokenCreate = "service"

    val addressIndex: Int = config.getParameters.get("addressIndex").toInt

    val prover: ErgoProver = ctx
      .newProverBuilder()
      .withMnemonic(
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
      case "service"   => new ErgoToken(directBox.get(0).getId, 1L)
      case "lend"      => new ErgoToken(directBox.get(0).getId, 1000000000L)
      case "repayment" => new ErgoToken(directBox.get(0).getId, 1000000000L)
    }

    val tokenBox: OutBox = tokenCreate match {
      case "service" =>
        txB
          .outBoxBuilder()
          .value(ErgCommons.MinBoxFee)
          .mintToken(token, nftName, nftDesc, 0)
          .contract(ContractUtils.sendToPK(ownerAddress))
          .build()

      case "lend" =>
        txB
          .outBoxBuilder()
          .value(ErgCommons.MinBoxFee)
          .mintToken(token, lendTokenName, lendTokenDesc, 0)
          .contract(ContractUtils.sendToPK(ownerAddress))
          .build()

      case "repayment" =>
        txB
          .outBoxBuilder()
          .value(ErgCommons.MinBoxFee)
          .mintToken(token, repaymentTokenName, repaymentTokenDesc, 0)
          .contract(ContractUtils.sendToPK(ownerAddress))
          .build()

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

  def mergeCreateServiceBox(
    ctx: BlockchainContext,
    config: ErgoToolConfig,
    nodeConfig: ErgoNodeConfig
  ): SignedTransaction = {
    val serviceNftBoxId: String = LendServiceTokens.serviceNFTId.toString
    val lendTokenId: String = LendServiceTokens.lendTokenId.toString
    val repaymentTokenId: String = LendServiceTokens.repaymentTokenId.toString

    val addressIndex: Int = config.getParameters.get("addressIndex").toInt
    val ownerAddress: Address = Address.createEip3Address(
      0,
      NodeConfig.networkType,
      SecretString.create(nodeConfig.getWallet.getMnemonic),
      SecretString.create("")
    )

    val creationInfo: CreationInfoRegister =
      CreationInfoRegister(creationHeight = ctx.getHeight.toLong)
    val serviceInfo: ServiceBoxInfoRegister = ServiceBoxInfoRegister(
      name = "ErgoLend",
      description = "A Lending Platform on Ergo"
    )
    val boxInfo: StringRegister = new StringRegister("SLEServiceBox")
    val ergoLendPubKeyRegister: SingleAddressRegister =
      new SingleAddressRegister(ServiceConfig.serviceOwner.toString)
    val profitSharingPercentageRegister: ProfitSharingRegister =
      ProfitSharingRegister(
        profitSharingPercentage = ServiceConfig.profitSharingPercentage,
        serviceFeeAmount = ServiceConfig.serviceFee
      )

    val prover: ErgoProver = ctx
      .newProverBuilder()
      .withMnemonic(
        SecretString.create(nodeConfig.getWallet.getMnemonic),
        SecretString.create(nodeConfig.getWallet.getPassword)
      )
      .withEip3Secret(addressIndex)
      .build()

    val spendingBoxes = ctx.getUnspentBoxesFor(ownerAddress, 0, 100)

    val spendingBoxesWithTokens: java.util.List[InputBox] = spendingBoxes
      .stream()
      .filter(!_.getTokens.isEmpty)
      .collect(Collectors.toList())

    val spendingBoxesWithServiceNft: java.util.List[InputBox] =
      spendingBoxesWithTokens
        .stream()
        .filter(_.getTokens.get(0).getId == ErgoId.create(serviceNftBoxId))
        .collect(Collectors.toList())

    val spendingBoxesWithLendTokensBoxes: java.util.List[InputBox] =
      spendingBoxesWithTokens
        .stream()
        .filter(_.getTokens.get(0).getId == ErgoId.create(lendTokenId))
        .collect(Collectors.toList())

    val spendingBoxesWithRepaymentTokensBoxes: java.util.List[InputBox] =
      spendingBoxesWithTokens
        .stream()
        .filter(_.getTokens.get(0).getId == ErgoId.create(repaymentTokenId))
        .collect(Collectors.toList())

//    val inputBoxes = List(
//      spendingBoxesWithServiceNft.get(0),
//      spendingBoxesWithLendTokensBoxes.get(0),
//      spendingBoxesWithRepaymentTokensBoxes.get(0)).asJava

    val amountToSend: Long = ErgCommons.MinBoxFee

    val txB = ctx.newTxBuilder()

    val serviceBoxContract = SLEServiceBoxContract.getContract(ctx)

    val serviceBox = txB.outBoxBuilder
      .value(amountToSend)
      .contract(serviceBoxContract)
      .tokens(
        spendingBoxesWithServiceNft.get(0).getTokens.get(0),
        spendingBoxesWithLendTokensBoxes.get(0).getTokens.get(0),
        spendingBoxesWithRepaymentTokensBoxes.get(0).getTokens.get(0)
      )
      .registers(
        creationInfo.toRegister,
        serviceInfo.toRegister,
        boxInfo.toRegister,
        ergoLendPubKeyRegister.toRegister,
        profitSharingPercentageRegister.toRegister
      )
      .build()

//    val coveringBoxes = ctx.getCoveringBoxesFor(ownerAddress, Parameters.MinFee, null).getBoxes.get(0)
    val inputBoxes = List(
      spendingBoxesWithServiceNft.get(0),
      spendingBoxesWithLendTokensBoxes.get(0),
      spendingBoxesWithRepaymentTokensBoxes.get(0)
    ).asJava

    val tx = txB
      .boxesToSpend(inputBoxes)
      .outputs(serviceBox)
      .fee(Parameters.MinFee)
      .sendChangeTo(ownerAddress.getErgoAddress)
      .build()

    val signed = prover.sign(tx)

    signed
  }

  def redeemServiceBox(
    ctx: BlockchainContext,
    config: ErgoToolConfig,
    node: ErgoNodeConfig
  ): SignedTransaction = {
    val serviceBoxAddress = Address.create(
      "8dxpYjSzdPQcqZzWFQJiAnVP8rESdCATsBquNWC6nd21nSLd1YkxD9Y2M3XqqVKSNyv4RycSrZnf9SPNrAiJ2jNPHv9qssP7D4Rr5m2H3Kcf2gG9Eobuxifx1xHxd1oZQHzS8iGCHveYHtHYTociT93MPoxXs2n75myZLEShpqNFPMtW3Sfb4mBWjjQGheARDvwxGwsA6DqJ9VGTdxFjwvJ5mBrLgEgoZoiSdETx9ZGtCQuqPWTr5r8jqPPUDykMnA97z2Sg63oFHBHzxJFRwhtWg2yKZ97CLL7q2NFZ8eUjrGW4JejfFv1M7r5HsendhhKTZbuAoHFNXqYPie3ecVPAPh4Z5ihQovvheBXekgyAV46geWXbbjg5ZBBB5Rkw4C31ycepMX1ni5tdPdhWcM1SnZuekRWWovxAy8M8RPYc2dTmPo6ephFAYMaVZmLAzv6VFMbji2JtQ1j4sMfXenPBcxvyksgw2Wc9Ekozj2YGNeA2zBWWdXzsiXKhJvrm6GVfGCSBXTmt2o3nUVM9tVkkP2nM4bHoEbZEoBvXRiK1CbE5ZSdPfAjrU3FWufuTEtBf3QV48ydArvjND36ZBSeLHK4s8wJQYbzJyKN3kkS9xCGHLCiWwGATWQMzdsV2z3SS"
    )
    val spendingBoxes = ctx.getUnspentBoxesFor(serviceBoxAddress, 0, 100)

    val ownerAddress = Address.createEip3Address(
      0,
      NodeConfig.networkType,
      SecretString.create(node.getWallet.getMnemonic),
      SecretString.create("")
    )

    val prover = ctx
      .newProverBuilder()
      .withMnemonic(
        SecretString.create(node.getWallet.getMnemonic),
        SecretString.create(node.getWallet.getPassword)
      )
      .withEip3Secret(config.getParameters.get("addressIndex").toInt)
      .build()

    val txB = ctx.newTxBuilder()
    val serviceBox = spendingBoxes.get(0)

    val boxOperations = BoxOperations.createForSender(ownerAddress)
    val coveringBoxes =
      boxOperations.withAmountToSpend(Parameters.MinFee).loadTop(ctx)

    val inputBoxes = List(serviceBox).asJava
    inputBoxes.addAll(coveringBoxes)

    val serviceBoxRedeemed = txB.outBoxBuilder
      .value(serviceBox.getValue)
      .contract(ContractUtils.sendToPK(ownerAddress))
      .tokens(
        serviceBox.getTokens.get(0),
        serviceBox.getTokens.get(1),
        serviceBox.getTokens.get(2)
      )
      .build()

    val tx = txB
      .boxesToSpend(inputBoxes)
      .outputs(serviceBoxRedeemed)
      .fee(Parameters.MinFee)
      .sendChangeTo(ownerAddress.getErgoAddress)
      .build()

    val signed = prover.sign(tx)
    signed
  }

//  def verifyServiceBox(ctx: BlockchainContext, config: ErgoToolConfig, nodeConfig: ErgoNodeConfig): SignedTransaction = {}
}
