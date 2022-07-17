package SLTokens.txs

import SLErgs.goal
import SLTokens.boxes.{SLTRepaymentBox, SLTServiceBox}
import SLTokens.{createGenesisServiceBox, createWrappedSLTRepaymentBox}
import common.ErgoTestBase
import commons.ergo.ErgCommons
import org.ergoplatform.appkit.{InputBox, SignedTransaction, UnsignedTransactionBuilder}

class SLTRepaymentFundDistributionSpec extends ErgoTestBase {
  "Repayment Fund Distribution Tx" should {
    val wrappedSLTServiceBox: SLTServiceBox = createGenesisServiceBox()
    val wrappedSLTRepaymentBox: SLTRepaymentBox = createWrappedSLTRepaymentBox(
      value = ErgCommons.MinBoxFee * 4,
      sigUSDAmount = goal / 2
    )
    "distribute funds to Lender" in {
      client.getClient.execute(implicit ctx => {
        val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
        val inputBoxes: Seq[InputBox] = Seq(wrappedSLTRepaymentBox.getAsInputBox(ctx, txB, dummyTxId, 0))
        val dataInputs: Seq[InputBox] = Seq(wrappedSLTServiceBox.getAsInputBox(ctx, txB, dummyTxId, 0))
        val sltRepaymentFundDistributionTx: SLTRepaymentFundDistributionTx = SLTRepaymentFundDistributionTx(
          inputBoxes,
          dataInputs)
        val signedTx: SignedTransaction = sltRepaymentFundDistributionTx.signTx
      })
    }
  }
}
