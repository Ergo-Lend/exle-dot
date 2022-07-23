package SLTokens.txs

import SLTokens.boxes.{SLTLendBox, SLTRepaymentBox}
import SLTokens.{SLTTokens, createFundLendPaymentBox, createFundRepaymentPaymentBox, createGenesisServiceBox, createWrappedSLTLendBox, createWrappedSLTRepaymentBox}
import common.ErgoTestBase
import org.ergoplatform.appkit.{InputBox, UnsignedTransactionBuilder}

class SLTRepaymentFundTxSpec extends ErgoTestBase {
  "Fund Repayment Tx" should {
    client.getClient.execute { implicit ctx =>
      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
      val serviceBox: InputBox = createGenesisServiceBox().getAsInputBox(ctx, txB, dummyTxId, 0)
      val inRepaymentBox: SLTRepaymentBox = createWrappedSLTRepaymentBox()
      val repaymentInputBox: InputBox =
        inRepaymentBox.getOutBox(ctx, txB).convertToInputWith(dummyTxId, 0)
      val fundLendPaymentInputBox: InputBox = createFundRepaymentPaymentBox(
        repaymentInputBox.getId.toString,
        sigUSDValue = inRepaymentBox.fundingInfoRegister.fundingGoal
      )

      val inputBoxes: Seq[InputBox] =
        Seq(repaymentInputBox, fundLendPaymentInputBox)

      // Tx
      val sltRepaymentFundTx: SLTRepaymentFundTx =
        SLTRepaymentFundTx(inputBoxes, Seq(serviceBox))
      sltRepaymentFundTx.signTx

      val outBoxAsInputBox: Seq[InputBox] =
        sltRepaymentFundTx.getOutBoxesAsInputBoxes(dummyTxId)

      val outRepaymentBox: SLTLendBox = new SLTLendBox(outBoxAsInputBox.head)

      "repayment box correct" in {
        assert(inRepaymentBox.tokens.length == 1)
        assert(inRepaymentBox.tokens.head.getId == SLTTokens.repaymentTokenId)
        assert(inRepaymentBox.tokens.head.getValue == 1)

        assert(
          outRepaymentBox.fundingInfoRegister == inRepaymentBox.fundingInfoRegister
        )
        assert(
          outRepaymentBox.lendingProjectDetailsRegister == inRepaymentBox.lendingProjectDetailsRegister
        )
        assert(
          outRepaymentBox.borrowerRegister.address == inRepaymentBox.borrowerRegister.address,
          s"OutLendBox: ${outRepaymentBox.borrowerRegister.address}, InLendBox: ${inRepaymentBox.borrowerRegister.address}"
        )
        assert(
          outRepaymentBox.loanTokenIdRegister.value
            .sameElements(inRepaymentBox.loanTokenIdRegister.value)
        )

        assert(
          outRepaymentBox
            .tokens(1)
            .getValue == inRepaymentBox.fundingInfoRegister.fundingGoal
        )
        assert(!outRepaymentBox.singleLenderRegister.isEmpty)
      }
    }
  }
}
