package tools.runners.helpers

import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoClient,
  ErgoId,
  ErgoProver,
  ErgoWallet,
  InputBox,
  NetworkType,
  Parameters,
  RestApiErgoClient,
  SecretString
}
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import org.ergoplatform.appkit.impl.ErgoTreeContract

import java.util.stream.Collectors

/**
  * Spender
  * Get lendcore.boxes from explorer, and then tries to spend it.
  * Explorer -> Retrieve input lendcore.boxes
  * Output lendcore.boxes for spending.
  */
object Spender {
  val minFee = Parameters.MinFee

  def spend(proxyContractAddress: String, configFile: String): String = {

    // Node Configuration values
    val conf: ErgoToolConfig = ErgoToolConfig.load(configFile)
    val nodeConf: ErgoNodeConfig = conf.getNode
    val explorerUrl: String =
      RestApiErgoClient.getDefaultExplorerUrl(NetworkType.TESTNET)

    val refundAddress: Address = Address.create(nodeConf.getWallet.getMnemonic)
    val addressIndex: Int = conf.getParameters.get("addressIndex").toInt

    // create ergoClient instance
    val ergoClient: ErgoClient = RestApiErgoClient.create(nodeConf, explorerUrl)
    val txJson: String = ergoClient.execute { (ctx: BlockchainContext) =>
      val prover: ErgoProver = ctx.newProverBuilder
        .withMnemonic(
          SecretString.create(nodeConf.getWallet.getMnemonic),
          SecretString.create(nodeConf.getWallet.getPassword)
        )
        .withEip3Secret(addressIndex)
        .build()

      val wallet: ErgoWallet = ctx.getWallet

      //1. Input is lendcore.boxes at proxy contract address
      val spendingAddress: Address = Address.create(proxyContractAddress)
      val spendingBoxes: java.util.List[InputBox] = ctx
        .getUnspentBoxesFor(spendingAddress, 0, 20)
        .stream()
        .collect(Collectors.toList())

      val refundAmount: Long = spendingBoxes.get(0).getValue - minFee

      val txB = ctx.newTxBuilder

      //2. Output box = wallet pk
      val refundBox = txB
        .outBoxBuilder()
        .value(refundAmount)
        .contract(
          new ErgoTreeContract(
            refundAddress.getErgoAddress.script,
            NetworkType.MAINNET
          )
        )
        .build()

      //3. run transaction
      val tx = txB
        .boxesToSpend(spendingBoxes)
        .outputs(refundBox)
        .fee(minFee)
        .sendChangeTo(prover.getP2PKAddress)
        .build()

      val signed = prover.sign(tx)

      val txId = ctx.sendTransaction(signed)

      signed.toJson(true)
    }

    txJson
  }

  def main(args: Array[String]): Unit = {
    val txJson: String = spend("", "")
    System.out.println(txJson)
  }
}
