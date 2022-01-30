package features.lend.txs.singleLender

import features.lend.boxes.FundsToAddressBox
import org.ergoplatform.appkit.{Address, BlockchainContext, InputBox, Parameters, SignedTransaction}

import scala.collection.JavaConverters.seqAsJavaListConverter


/**
 *
 */
class ProxyContractTx(val paymentBox: InputBox,
                      val funderAddress: String) {
  def runTx(ctx: BlockchainContext): SignedTransaction = {
    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()

    val refundToLenderBox = new FundsToAddressBox(
      paymentBox.getValue - Parameters.MinFee, funderAddress)
      .getOutputBox(ctx, txB)

    val inputBox = List(paymentBox).asJava

    val refundTx = txB.boxesToSpend(inputBox)
      .fee(Parameters.MinFee)
      .outputs(refundToLenderBox)
      .sendChangeTo(Address.create(funderAddress).getErgoAddress)
      .build()

    val signedTx = prover.sign(refundTx)

    signedTx
  }
}
