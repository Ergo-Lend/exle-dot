package features.lend.processor

import config.Configs
import ergotools.client.Client
import errors.connectionException
import features.lend.LendBoxExplorer
import helpers.StackTrace
import org.ergoplatform.appkit.{Address, BlockchainContext}
import play.api.Logger
import special.collection.Coll

import javax.inject.Inject

class FinalizeRequestProcessor @Inject()(client: Client, lendBoxExplorer: LendBoxExplorer) {
  private val logger: Logger = Logger(this.getClass)

  def processRefundLend(ctx: BlockchainContext): Unit = {
  }

  def processActiveLend(ctx: BlockchainContext): Unit = {
//    try {
//      client.getAllUnspentBox(Address.create(
//        Configs.addressEncoder.fromProposition(
//          addresses.getRaffleActiveContract().getErgoTree).get.toString))
//        .filter(box => {
//          box.getRegisters.get(0).getValue.asInstanceOf[Coll[Long]].toArray(4) < client.getHeight
//        }).foreach(raffle => {
//        if (!lendBoxExplorer.isBoxInMemPool(raffle)) processSingleRaffle(ctx, raffle)
//      })
//    } catch {
//      case e: connectionException => logger.warn(e.getMessage)
//      case e: Throwable => logger.error(StackTrace.getStackTraceStr(e))
//    }
  }

  def processRepayment(context: BlockchainContext): Unit = {
  }

  def Refund(): Unit = {
    client.getClient.execute((ctx: BlockchainContext) => {
      processRefundLend(ctx)
      processActiveLend(ctx)
      processRepayment(ctx)
    })
  }
}
