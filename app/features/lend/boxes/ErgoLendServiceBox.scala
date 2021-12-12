package features.lend.boxes

import config.Configs
import features.lend.contracts.lendServiceBoxScript
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ErgoContract, OutBox}

/**
 * Service box
 * Contains token
 */
class ErgoLendServiceBox extends Box {
  def getOutputBox(ctx: BlockchainContext): OutBox = {
    val txB = ctx.newTxBuilder()
    val serviceBoxContract = getServiceBoxContract(ctx: BlockchainContext)
    val serviceBox = txB.outBoxBuilder()
      .value(Configs.fee * 2)
      .contract(serviceBoxContract)
      .build()

    serviceBox
  }

  def getServiceBoxContract(ctx: BlockchainContext): ErgoContract = {
    ctx.compileContract(ConstantsBuilder.create()
      .item("fee", Configs.fee)
      .build(), lendServiceBoxScript)
  }
}