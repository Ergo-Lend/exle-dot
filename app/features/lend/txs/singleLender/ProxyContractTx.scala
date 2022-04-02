package features.lend.txs.singleLender

import errors.proveException
import features.lend.boxes.FundsToAddressBox
import org.ergoplatform.appkit.{Address, BlockchainContext, InputBox, Parameters, SignedTransaction}

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.mutable


/**
 *
 */
class RefundProxyContractTx(val paymentBoxes: mutable.Buffer[InputBox],
                            val funderAddress: String) {
  def runTx(ctx: BlockchainContext): SignedTransaction = {
    val txB = ctx.newTxBuilder()
    val prover = ctx.newProverBuilder().build()

    val paymentBoxValue = paymentBoxes.foldLeft(0L)((sum, inputBox) => sum + inputBox.getValue)

    val refundToLenderBox = new FundsToAddressBox(
      paymentBoxValue - Parameters.MinFee, funderAddress)
      .getOutputBox(ctx, txB)

    val inputBox = paymentBoxes.asJava

    val refundTx = txB.boxesToSpend(inputBox)
      .fee(Parameters.MinFee)
      .outputs(refundToLenderBox)
      .sendChangeTo(Address.create(funderAddress).getErgoAddress)
      .build()

    try {
      val signedTx = prover.sign(refundTx)
      signedTx
    } catch {
      case e: proveException => throw new proveException()
    }

  }
}
