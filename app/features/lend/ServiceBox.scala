package features.lend

import config.Configs
import features.lend.boxes.Box
import features.lend.contracts.lendServiceBoxScript
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ErgoContract, OutBox}

class ServiceBox extends Box {
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
