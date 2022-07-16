package SLTokens.txs

import SLTokens.boxes.{SLTLendBox, SLTRepaymentBox}
import SLTokens.{
  createFundLendPaymentBox,
  createWrappedSLTLendBox,
  createWrappedSLTRepaymentBox,
  SLTTokens
}
import common.ErgoTestBase
import org.ergoplatform.appkit.{InputBox, OutBox, UnsignedTransactionBuilder}

class SLTLendFundTxSpec extends ErgoTestBase {
  "Fund Lend Tx" should {
    client.getClient.execute { implicit ctx =>
      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
      val inLendBox: SLTRepaymentBox = createWrappedSLTRepaymentBox()
      val lendInputBox: InputBox =
        inLendBox.getOutBox(ctx, txB).convertToInputWith(dummyTxId, 0)
      val fundLendPaymentInputBox: InputBox = createFundLendPaymentBox(
        lendInputBox.getId.toString,
        value = inLendBox.fundingInfoRegister.fundingGoal
      )

      val inputBoxes: Seq[InputBox] = Seq(lendInputBox, fundLendPaymentInputBox)

      val sltLendFundTx: SLTLendFundTx = SLTLendFundTx(inputBoxes)
      sltLendFundTx.signTx

      val outBoxAsInputBox: Seq[InputBox] =
        sltLendFundTx.getOutBoxesAsInputBoxes(dummyTxId)

      val outLendBox: SLTLendBox = new SLTLendBox(outBoxAsInputBox.head)

      "lend box correct" in {
        assert(inLendBox.tokens.length == 1)
        assert(inLendBox.tokens.head.getId == SLTTokens.lendTokenId)
        assert(inLendBox.tokens.head.getValue == 1)

        assert(outLendBox.fundingInfoRegister == inLendBox.fundingInfoRegister)
        assert(
          outLendBox.lendingProjectDetailsRegister == inLendBox.lendingProjectDetailsRegister
        )
        assert(
          outLendBox.borrowerRegister.address == inLendBox.borrowerRegister.address,
          s"OutLendBox: ${outLendBox.borrowerRegister.address}, InLendBox: ${inLendBox.borrowerRegister.address}"
        )
        assert(
          outLendBox.loanTokenIdRegister.value
            .sameElements(inLendBox.loanTokenIdRegister.value)
        )

        assert(
          outLendBox
            .tokens(1)
            .getValue == inLendBox.fundingInfoRegister.fundingGoal
        )
        assert(!outLendBox.singleLenderRegister.isEmpty)
      }
    }
  }
}
