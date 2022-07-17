package SLTokens.txs

import SLTokens.boxes.{SLTLendBox, SLTServiceBox}
import SLTokens.{createGenesisServiceBox, createInitiationPaymentBox, SLTTokens}
import common.ErgoTestBase
import commons.configs.ServiceConfig
import org.ergoplatform.appkit.{InputBox, OutBox, UnsignedTransactionBuilder}

class SLTLendInitiationTxSpec extends ErgoTestBase {

  "Initiation Tx" should {
    client.getClient.execute { implicit ctx =>
      // 1. Create Input Boxes
      //    a. Service Box
      //    b. Payment Box

      // 2. Create Tx: SLTLendInitiationTx
      // 3. Get OutBox
      // 4. Check
      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
      val inSLTServiceBox: SLTServiceBox = createGenesisServiceBox()
      val initiationPaymentInputBox: InputBox = createInitiationPaymentBox()
      val serviceInputBox: InputBox =
        inSLTServiceBox.getOutBox(ctx, txB).convertToInputWith(dummyTxId, 0)

      // Create InputBoxes
      val inputBoxes: Seq[InputBox] =
        Seq(serviceInputBox, initiationPaymentInputBox)

      // Create Tx and sign
      val sltLendInitiationTx: SLTLendInitiationTx =
        SLTLendInitiationTx(inputBoxes)
      sltLendInitiationTx.signTx

      // Get out boxes of tx
      val outBoxes: Seq[OutBox] = sltLendInitiationTx.getOutBoxes
      val outBoxAsInputBox: Seq[InputBox] =
        sltLendInitiationTx.getOutBoxesAsInputBoxes(dummyTxId)
      val lendOutBox: OutBox = outBoxes(1)

      // Wrap out boxes
      val outSLTServiceBox: SLTServiceBox =
        new SLTServiceBox(outBoxAsInputBox.head)
      val outSLTLendBox: SLTLendBox = new SLTLendBox(outBoxAsInputBox(1))

      "have service fee" in {
        assert(outBoxAsInputBox(2).getValue == ServiceConfig.serviceFee)
        assert(
          outBoxAsInputBox(2).getErgoTree == ServiceConfig.serviceOwner.getErgoAddress.script
        )
      }

      "service box correct" in {
        // Service Box Check
        assert(
          outSLTServiceBox.tokens.head.getId == inSLTServiceBox.tokens.head.getId
        )
        assert(
          outSLTServiceBox.tokens.head.getValue == inSLTServiceBox.tokens.head.getValue
        )
        assert(
          outSLTServiceBox.tokens(1).getId == inSLTServiceBox.tokens(1).getId
        )
        assert(
          outSLTServiceBox
            .tokens(1)
            .getValue == inSLTServiceBox.tokens(1).getValue - 1
        )
        assert(
          outSLTServiceBox.tokens(2).getId == inSLTServiceBox.tokens(2).getId
        )
        assert(
          outSLTServiceBox.tokens(2).getValue == inSLTServiceBox
            .tokens(2)
            .getValue
        )

      }

      "lend box correct" in {
        assert(outSLTLendBox.tokens.head.getValue == 1)
        assert(outSLTLendBox.tokens.head.getId == SLTTokens.lendTokenId)
        assert(
          initiationPaymentInputBox.getRegisters
            .get(0)
            .getValue == lendOutBox.getRegisters.get(0).getValue
        )
        assert(
          initiationPaymentInputBox.getRegisters
            .get(1)
            .getValue == lendOutBox.getRegisters.get(1).getValue
        )
        assert(
          initiationPaymentInputBox.getRegisters
            .get(2)
            .getValue == lendOutBox.getRegisters.get(2).getValue
        )
        assert(
          initiationPaymentInputBox.getRegisters
            .get(3)
            .getValue == lendOutBox.getRegisters.get(3).getValue
        )
      }
    }
  }

  // How would other people hack the proxy contract?
  // Send it to box other than LendBox
}
