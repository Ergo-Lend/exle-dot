package SLTokens.txs

import SLErgs.goal
import SLTokens.boxes.{SLTLendBox, SLTRepaymentBox, SLTServiceBox}
import SLTokens.{createGenesisServiceBox, createWrappedSLTLendBox}
import common.ErgoTestBase
import commons.ergo.ErgCommons
import org.ergoplatform.appkit.{
  InputBox,
  SignedTransaction,
  UnsignedTransactionBuilder
}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class SLTLendToRepaymentTxSpec extends ErgoTestBase {
  "Lend to Repayment Tx" should {
    val wrappedSLTServiceBox: SLTServiceBox = createGenesisServiceBox()
    val wrappedSLTLendBox: SLTLendBox = createWrappedSLTLendBox(
      value = ErgCommons.MinBoxFee * 3,
      sigUSDAmount = goal,
      lenderAddress = dummyAddress
    )

    "convert to repayment box" in {
      client.getClient.execute { implicit ctx =>
        val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
        val serviceBoxAsInput: InputBox =
          wrappedSLTServiceBox.getAsInputBox(ctx, txB, dummyTxId, 0)
        val inputBoxes: Seq[InputBox] = Seq(
          serviceBoxAsInput,
          wrappedSLTLendBox.getAsInputBox(ctx, txB, dummyTxId, 0)
        )

        val sltLendToRepaymentTx: SLTLendToRepaymentTx =
          SLTLendToRepaymentTx(inputBoxes)
        val signedTx: SignedTransaction = sltLendToRepaymentTx.signTx
        val outBoxes: Seq[InputBox] = signedTx.getOutputsToSpend.asScala.toSeq

        val outSLTServiceBox: SLTServiceBox = new SLTServiceBox(outBoxes.head)
        val outRepaymentBox: SLTRepaymentBox = new SLTRepaymentBox(outBoxes(1))

        assert(
          outSLTServiceBox.tokens(1).getValue - 1 == wrappedSLTServiceBox
            .tokens(1)
            .getValue
        )
        assert(
          outSLTServiceBox.tokens(2).getValue + 1 == wrappedSLTServiceBox
            .tokens(2)
            .getValue
        )
        assert(
          outRepaymentBox.fundingInfoRegister
            .equals(wrappedSLTLendBox.fundingInfoRegister)
        )
        assert(
          outRepaymentBox.lendingProjectDetailsRegister
            .equals(wrappedSLTLendBox.lendingProjectDetailsRegister)
        )
        assert(
          outRepaymentBox.singleLenderRegister
            .equals(wrappedSLTLendBox.singleLenderRegister)
        )
        assert(
          outRepaymentBox.borrowerRegister.address
            .equals(wrappedSLTLendBox.borrowerRegister.address)
        )
        assert(
          outRepaymentBox.loanTokenIdRegister.value
            .sameElements(wrappedSLTLendBox.loanTokenIdRegister.value)
        )
      }
    }
  }
}
