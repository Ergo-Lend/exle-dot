package SLTokens.boxes

import SLErgs.{
  deadlineHeightLength,
  goal,
  interestRate,
  loanDescription,
  loanName,
  repaymentHeightLength
}
import SLTokens.TxFeeCosts
import common.ErgoTestBase
import commons.boxes.registers.RegisterTypes.{AddressRegister, CollByteRegister}
import commons.registers.{
  BorrowerRegister,
  FundingInfoRegister,
  LendingProjectDetailsRegister,
  SingleLenderRegister
}
import org.ergoplatform.appkit.{Address, ErgoToken, InputBox}
import tokens.SigUSD

class SLTProxyBoxesSpec extends ErgoTestBase {
  "Create SLT Proxy Box" should {
    "have the right components in its class" in {
      client.getClient.execute { implicit ctx =>
        val borrowerAddress: String = dummyAddress.getErgoAddress.toString
        val borrowerRegister: BorrowerRegister =
          new BorrowerRegister(borrowerAddress)
        val fundingInfoRegister: FundingInfoRegister = FundingInfoRegister(
          fundingGoal = goal,
          deadlineHeight = ctx.getHeight + deadlineHeightLength,
          interestRatePercent = interestRate,
          repaymentHeightLength = repaymentHeightLength,
          creationHeight = ctx.getHeight
        )
        val lendingProjectDetailsRegister: LendingProjectDetailsRegister =
          LendingProjectDetailsRegister(
            projectName = loanName,
            description = loanDescription
          )
        val loanTokenIdRegister: CollByteRegister =
          new CollByteRegister(SigUSD.id.getBytes)

        // Get Proxy Box
        val sltCreateLendProxyBox: SLTCreateLendProxyBox =
          SLTCreateLendProxyBox.getBox(
            borrowerPk = borrowerAddress,
            loanToken = SigUSD.id.getBytes,
            projectName = loanName,
            description = loanDescription,
            deadlineHeight = deadlineHeightLength,
            goal = goal,
            interestRate = interestRate,
            repaymentHeightLength = repaymentHeightLength
          )

        val proxyBoxAsInputBox: InputBox = sltCreateLendProxyBox.getAsInputBox(
          ctx,
          ctx.newTxBuilder(),
          dummyTxId,
          0
        )

        // ReWrap Proxy box
        val wrappedOutProxyBox: SLTCreateLendProxyBox =
          new SLTCreateLendProxyBox(proxyBoxAsInputBox)

        assert(
          wrappedOutProxyBox.fundingInfoRegister == fundingInfoRegister,
          "Funding Info"
        )
        assert(
          wrappedOutProxyBox.loanTokenIdRegister.value
            .sameElements(loanTokenIdRegister.value),
          "Loan Token Id"
        )
        assert(
          wrappedOutProxyBox.lendingProjectDetailsRegister == lendingProjectDetailsRegister,
          "Lending Project Details"
        )
        assert(
          wrappedOutProxyBox.borrowerRegister.borrowersAddress == borrowerRegister.borrowersAddress,
          "Borrowers Address"
        )

        assert(
          wrappedOutProxyBox.value == TxFeeCosts.creationTxFee,
          "Tx Fee (Value)"
        )
      }
    }
  }

  "Fund Lend Proxy Box" should {
    "have the right components in its class" in {
      client.getClient.execute { implicit ctx =>
        // We're using dummy TxId because we just want a random string
        val boxId: Array[Byte] = dummyTxId.getBytes
        val boxIdRegister: CollByteRegister = new CollByteRegister(boxId)

        val lenderAddress: Address = dummyAddress
        val singleLenderRegister: SingleLenderRegister =
          new SingleLenderRegister(lenderAddress)

        val tokens: Seq[ErgoToken] = Seq(SigUSD.apply(1000).toErgoToken)

        // Create wrapped SLTFundLendProxyBox
        val sltFundLendProxyBox: SLTFundLendProxyBox = SLTFundLendProxyBox
          .getBox(boxId, lenderAddress, tokens)

        // Get as Input Box
        val lendProxyBoxAsInputBox: InputBox = sltFundLendProxyBox
          .getAsInputBox(ctx, ctx.newTxBuilder(), dummyTxId, 0)

        val wrappedOutLendProxyBox: SLTFundLendProxyBox =
          new SLTFundLendProxyBox(
            lendProxyBoxAsInputBox
          )

        assert(
          wrappedOutLendProxyBox.boxIdRegister.value
            .sameElements(boxIdRegister.value),
          "Box Id Register"
        )
        assert(
          wrappedOutLendProxyBox.lenderRegister.lendersAddress
            .equals(singleLenderRegister.lendersAddress),
          "Lender Register"
        )
        assert(wrappedOutLendProxyBox.tokens == tokens, "Tokens")
        assert(
          wrappedOutLendProxyBox.value == TxFeeCosts.fundLendTxFee,
          "Tx Fee (Value)"
        )
      }
    }
  }

  "Fund Repayment Proxy Box" should {
    "have the right components in its class" in {
      client.getClient.execute { implicit ctx =>
        // We're using dummy TxId because we just want a random string
        val boxId: Array[Byte] = dummyTxId.getBytes
        val boxIdRegister: CollByteRegister = new CollByteRegister(boxId)

        val fundersAddress: Address = dummyAddress
        val funderRegister: AddressRegister =
          new SingleLenderRegister(fundersAddress)

        val tokens: Seq[ErgoToken] = Seq(SigUSD.apply(1000).toErgoToken)

        // Create wrapped SLTFundLendProxyBox
        val sltFundRepaymentProxyBox: SLTFundRepaymentProxyBox =
          SLTFundRepaymentProxyBox
            .getBox(boxId, fundersAddress, tokens)

        // Get as Input Box
        val repaymentProxyBoxAsInputBox: InputBox = sltFundRepaymentProxyBox
          .getAsInputBox(ctx, ctx.newTxBuilder(), dummyTxId, 0)

        val wrappedOutRepaymentProxyBox: SLTFundRepaymentProxyBox =
          new SLTFundRepaymentProxyBox(
            repaymentProxyBoxAsInputBox
          )

        assert(
          wrappedOutRepaymentProxyBox.boxIdRegister.value
            .sameElements(boxIdRegister.value),
          "Box Id Register"
        )
        assert(
          wrappedOutRepaymentProxyBox.fundersAddress.address
            .equals(funderRegister.address),
          "Funder Register"
        )
        assert(wrappedOutRepaymentProxyBox.tokens == tokens, "Tokens")
        assert(
          wrappedOutRepaymentProxyBox.value == TxFeeCosts.fundRepaymentTxFee(),
          "Tx Fee (Value)"
        )
      }
    }
  }
}
