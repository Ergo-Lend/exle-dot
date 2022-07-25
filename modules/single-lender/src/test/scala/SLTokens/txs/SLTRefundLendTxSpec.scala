package SLTokens.txs

import SLTokens.boxes.{SLTLendBox, SLTServiceBox}
import SLTokens.{createGenesisServiceBox, createWrappedSLTLendBox}
import common.ErgoTestBase
import org.ergoplatform.appkit.{
  InputBox,
  SignedTransaction,
  UnsignedTransactionBuilder
}

/**
  * In this test, we will need 2 components mainly.
  * LendBox, and ServiceBox
  */
class SLTRefundLendTxSpec extends ErgoTestBase {
  "Lend Refund Tx" should {
    val sltServiceBox: SLTServiceBox = createGenesisServiceBox()
    val sltLendBox: SLTLendBox =
      createWrappedSLTLendBox(deadlineHeightLength = -100)

    "absorb LendBox" in {
      client.getClient.execute { implicit ctx =>
        val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
        val inputBoxes: Seq[InputBox] = Seq(
          sltServiceBox.getAsInputBox(ctx, txB, dummyTxId, 0),
          sltLendBox.getAsInputBox(ctx, txB, dummyTxId, 0)
        )

        val sltLendRefundTx: SLTLendRefundTx = SLTLendRefundTx(inputBoxes)

        val signedTx: SignedTransaction = sltLendRefundTx.signTx

        // Service Box and Mining Fee only
        assert(signedTx.getOutputsToSpend.size() == 2)
        val outSLTServiceBox: SLTServiceBox =
          new SLTServiceBox(signedTx.getOutputsToSpend.get(0))
        assert(
          sltServiceBox.tokens(1).getValue + 1 == outSLTServiceBox
            .tokens(1)
            .getValue
        )
      }
    }
  }
}
