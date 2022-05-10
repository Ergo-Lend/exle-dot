package contracts

import config.Configs
import node.Client
import org.ergoplatform.appkit.{BlockchainContext, Constants, ConstantsBuilder, ErgoContract, Parameters}

class ProxyContracts(client: Client) {
  val minFee: Long = Parameters.MinFee
  val refundHeightThresholdValue: Long = ((Configs.creationDelay / 60 / 2) + 1).toLong

  def getRefundHeightThreshold: Long = {
    client.getHeight + refundHeightThresholdValue
  }

  def encodeAddress(contract: ErgoContract): String = {
    Configs.addressEncoder.fromProposition(contract.getErgoTree).get.toString
  }

  def toString(contract: ErgoContract): String = {
    encodeAddress(contract)
  }

  def compile(constantsBuilder: Constants, contract: String): ErgoContract = {
    try {
      client.getClient.execute((ctx: BlockchainContext) => ctx.compileContract(constantsBuilder, contract))
    } catch {
      case e: Exception => throw e
    }
  }
}