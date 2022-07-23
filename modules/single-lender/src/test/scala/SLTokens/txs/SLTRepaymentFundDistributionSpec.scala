package SLTokens.txs

import SLErgs.goal
import SLTokens.boxes.{SLTRepaymentBox, SLTRepaymentDistribution, SLTServiceBox}
import SLTokens.{
  createGenesisServiceBox,
  createWrappedSLTRepaymentBox,
  SLTTokens
}
import boxes.FundsToAddressBox
import common.ErgoTestBase
import commons.ergo.ErgCommons
import org.ergoplatform.appkit.{InputBox, UnsignedTransactionBuilder}

class SLTRepaymentFundDistributionSpec extends ErgoTestBase {
  "Repayment Fund Distribution Tx" should {
    val wrappedSLTServiceBox: SLTServiceBox = createGenesisServiceBox()
    val wrappedSLTRepaymentBox: SLTRepaymentBox = createWrappedSLTRepaymentBox(
      value = ErgCommons.MinBoxFee * 4,
      sigUSDAmount = goal
    )
    "distribute funds to Lender" in {
      client.getClient.execute { implicit ctx =>
        val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
        val inputBoxes: Seq[InputBox] =
          Seq(wrappedSLTRepaymentBox.getAsInputBox(ctx, txB, dummyTxId, 0))
        val dataInputs: Seq[InputBox] =
          Seq(wrappedSLTServiceBox.getAsInputBox(ctx, txB, dummyTxId, 0))
        val sltRepaymentFundDistributionTx: SLTRepaymentFundDistributionTx =
          SLTRepaymentFundDistributionTx(inputBoxes, dataInputs)
        sltRepaymentFundDistributionTx.signTx
        val outBoxes: Seq[InputBox] =
          sltRepaymentFundDistributionTx.getOutBoxesAsInputBoxes(dummyTxId)

        val outRepaymentBox: SLTRepaymentBox =
          new SLTRepaymentBox(outBoxes.head)
        val outLenderBox: FundsToAddressBox = new FundsToAddressBox(outBoxes(1))
        val outProtocolOwnerBox: FundsToAddressBox =
          new FundsToAddressBox(outBoxes(2))

        // [LendersShare, ProtocolOwner]
        val repaymentShare: Seq[Long] =
          SLTRepaymentDistribution.calculateRepayment(
            wrappedSLTRepaymentBox.tokens(1).getValue,
            wrappedSLTRepaymentBox.fundingInfoRegister.interestRatePercent,
            wrappedSLTServiceBox.profitSharingRegister.profitSharingPercentage
          )

        // Repayment Check
        assert(
          wrappedSLTRepaymentBox.tokens.head == outRepaymentBox.tokens.head
        )
        assert(
          wrappedSLTRepaymentBox.tokens.head.getId == SLTTokens.repaymentTokenId
        )
        assert(outRepaymentBox.tokens.length == 1)
        assert(
          wrappedSLTRepaymentBox.repaymentDetailsRegister.repaymentPaid == 0
        )
        assert(
          outRepaymentBox.repaymentDetailsRegister.repaymentPaid ==
            wrappedSLTRepaymentBox.repaymentDetailsRegister.repaymentPaid + wrappedSLTRepaymentBox
              .tokens(1)
              .getValue
        )

        // lenderbox check
        assert(outLenderBox.tokens.head.getValue == repaymentShare.head)
        assert(
          outLenderBox.address.toString == wrappedSLTRepaymentBox.singleLenderRegister.lendersAddress
        )

        // protocolOwnerBox check
        assert(outProtocolOwnerBox.tokens.head.getValue == repaymentShare(1))
        assert(
          outProtocolOwnerBox.address.toString == wrappedSLTServiceBox.exlePubKey.address
        )
      }
    }
  }
}
