package ergotools

import org.ergoplatform.appkit.Parameters.MinFee
import org.ergoplatform.appkit.{BlockchainContext, BoxOperations, ErgoClient, ErgoContracts, ErgoToken}

class ServiceBoxCreator(tokenAmount: Long, tokenName: String, tokenDesc: String, tokenNumberOfDecimals: Int) {

  def create(ctx: BlockchainContext): Unit = {
//      val token = new ErgoToken(boxesToSpend.get(0).getId, tokenAmount)
//      val txB = ctx.newTxBuilder
//      val newBox = txB.outBoxBuilder
//        .value(ergAmount)
//        .mintToken(token, tokenName,tokenDesc, tokenNumberOfDecimals)
//        .contract(ErgoContracts.sendToPK(ctx, sender))
//        .build()
//      val tx = txB
//        .boxesToSpend(boxesToSpend).outputs(newBox)
//        .fee(MinFee)
//        .sendChangeTo(senderProver.getP2PKAddress)
//        .build()
//      val signed = loggedStep(s"Signing the transaction", console) {
//        senderProver.sign(tx)
//      }
//      val txJson = signed.toJson(true)
  }
}
