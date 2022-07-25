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

class SLTFundLendTxSpec extends ErgoTestBase {
  "Fund Lend Tx" should {
    client.getClient.execute { implicit ctx =>
      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()

      // Create a wrapped SLT Lend Box
      val inLendBox: SLTLendBox = createWrappedSLTLendBox()

      // Turn the LendBox into an InputBox
      val lendInputBox: InputBox =
        inLendBox.getOutBox(ctx, txB).convertToInputWith(dummyTxId, 0)

      // Create a PaymentBox to fund the LendBox
      val fundLendPaymentInputBox: InputBox = createFundLendPaymentBox(
        lendBoxId = lendInputBox.getId.getBytes,
        value = inLendBox.fundingInfoRegister.fundingGoal
      )

      // Put both boxes into a Seq
      val inputBoxes: Seq[InputBox] = Seq(lendInputBox, fundLendPaymentInputBox)

      // Create the FundLendTx and sign the tx
      val sltLendFundTx: SLTLendFundTx = SLTLendFundTx(inputBoxes)
      sltLendFundTx.signTx

      // Get the outboxes as Input Boxes
      val outBoxAsInputBox: Seq[InputBox] =
        sltLendFundTx.getOutBoxesAsInputBoxes(dummyTxId)

      // Wrap the OutBox LendBox
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
