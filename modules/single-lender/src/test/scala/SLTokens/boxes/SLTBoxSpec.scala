package SLTokens.boxes

import SLTokens.{SLTTokens, createFundRepaymentPaymentBox, createGenesisServiceBox, createWrappedSLTLendBox, createWrappedSLTRepaymentBox}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import SLErgs.{client, dummyAddress, dummyTxId, goal, interestRate, loanDescription, loanName, repaymentHeightLength}
import boxes.FundsToAddressBox
import commons.configs.ServiceConfig.serviceOwner
import commons.configs.{ServiceConfig, Tokens}
import org.ergoplatform.appkit.{ErgoToken, InputBox}
import tokens.SigUSD

class SLTBoxSpec extends AnyWordSpec with Matchers {
  "SLTLendBox" should {
    val wrappedLendBox: SLTLendBox = createWrappedSLTLendBox()

    "get accurate outbox" in {
      checkLendBox(wrappedLendBox)

      assert(
        wrappedLendBox.singleLenderRegister.lendersAddress.isEmpty,
        "SLTLendBox: Lenders Address should be empty"
      )
    }

    "inputBox" in {
      client.getClient.execute { ctx =>
        val sltInputBox: InputBox =
          wrappedLendBox.getAsInputBox(ctx, ctx.newTxBuilder(), dummyTxId, 0)
        val wrappedSLTLendInputBox: SLTLendBox = new SLTLendBox(sltInputBox)

        assert(
          wrappedSLTLendInputBox.singleLenderRegister.lendersAddress.isEmpty,
          "SLTLendBox: Lenders Address should be empty"
        )
        checkLendBox(wrappedSLTLendInputBox)
      }
    }

    def checkLendBox(wrappedLendBox: SLTLendBox): Unit = {
      assert(
        wrappedLendBox.fundingInfoRegister.fundingGoal == goal,
        "SLTLendBox: Goal Incorrect"
      )
      assert(
        wrappedLendBox.fundingInfoRegister.interestRatePercent == interestRate,
        "SLTLendBox: InterestRate Incorrect"
      )
      assert(
        wrappedLendBox.fundingInfoRegister.repaymentHeightLength == repaymentHeightLength,
        "SLTLendBox: RepaymentHeightLength Incorrect"
      )
      assert(
        wrappedLendBox.lendingProjectDetailsRegister.projectName == loanName,
        "SLTLendBox: Loan Name Incorrect"
      )
      assert(
        wrappedLendBox.lendingProjectDetailsRegister.description == loanDescription,
        "SLTLendBox: Loan Description Incorrect"
      )
      assert(
        wrappedLendBox.loanTokenIdRegister.value
          .sameElements(SigUSD.id.getBytes),
        "SLTLendBox: Incorrect Loan Token"
      )
    }

    "funded Lendbox has lenders and correct value" in {
      client.getClient.execute { ctx =>
        val fundedLendInputBox = SLTLendBox
          .getFunded(wrappedLendBox, dummyAddress)
          .getAsInputBox(ctx, ctx.newTxBuilder(), dummyTxId, 0)
        val wrappedLendInputBox = new SLTLendBox(fundedLendInputBox)

        checkLendBox(wrappedLendInputBox)
        assert(
          wrappedLendInputBox.singleLenderRegister.lendersAddress
            .equals(dummyAddress.getErgoAddress.toString)
        )
        assert(
          wrappedLendInputBox.tokens(1).getId.equals(SigUSD.id),
          "SLTLendBox: Loan Token incorrect"
        )
        assert(
          wrappedLendInputBox
            .tokens(1)
            .getValue == wrappedLendInputBox.fundingInfoRegister.fundingGoal,
          "SLTLendBox: Funded Value incorrect"
        )
      }
    }
  }

  "SLTRepaymentBox" should {
    val wrappedRepaymentBox: SLTRepaymentBox = createWrappedSLTRepaymentBox()

    "get accurate outbox" in {
      checkLendBoxDetails(wrappedRepaymentBox)
      checkRepaymentDetails(wrappedRepaymentBox)
    }

    "inputBox" in {
      client.getClient.execute { ctx =>
        val sltInputBox: InputBox =
          wrappedRepaymentBox.getAsInputBox(
            ctx,
            ctx.newTxBuilder(),
            dummyTxId,
            0
          )
        val wrappedSLTRepaymentInputBox: SLTRepaymentBox =
          new SLTRepaymentBox(sltInputBox)

        checkLendBoxDetails(wrappedSLTRepaymentInputBox)
        checkRepaymentDetails(wrappedSLTRepaymentInputBox)
      }
    }

    def checkLendBoxDetails(wrappedRepaymentBox: SLTRepaymentBox): Unit = {
      assert(
        wrappedRepaymentBox.fundingInfoRegister.fundingGoal == goal,
        "SLTRepaymentBox: Goal Incorrect"
      )
      assert(
        wrappedRepaymentBox.fundingInfoRegister.interestRatePercent == interestRate,
        "SLTRepaymentBox: InterestRate Incorrect"
      )
      assert(
        wrappedRepaymentBox.fundingInfoRegister.repaymentHeightLength == repaymentHeightLength,
        "SLTRepaymentBox: RepaymentHeightLength Incorrect"
      )
      assert(
        wrappedRepaymentBox.lendingProjectDetailsRegister.projectName == loanName,
        "SLTRepaymentBox: Loan Name Incorrect"
      )
      assert(
        wrappedRepaymentBox.lendingProjectDetailsRegister.description == loanDescription,
        "SLTRepaymentBox: Loan Description Incorrect"
      )
      assert(
        wrappedRepaymentBox.loanTokenIdRegister.value
          .sameElements(SigUSD.id.getBytes),
        "SLTRepaymentBox: Incorrect Loan Token"
      )
      assert(
        wrappedRepaymentBox.borrowerRegister.borrowersAddress == dummyAddress.getErgoAddress.toString,
        "SLTRepaymentBox: Borrowers Address Incorrect"
      )
    }

    def checkRepaymentDetails(wrappedRepaymentBox: SLTRepaymentBox): Unit = {
      val interestAmount: Long =
        (wrappedRepaymentBox.fundingInfoRegister.fundingGoal * wrappedRepaymentBox.fundingInfoRegister.interestRatePercent) / 1000
      assert(
        wrappedRepaymentBox.singleLenderRegister.lendersAddress == dummyAddress.getErgoAddress.toString,
        "SLTRepaymentBox: Lenders Address Incorrect"
      )
      assert(
        wrappedRepaymentBox.repaymentDetailsRegister.totalInterestAmount == interestAmount,
        "SLTRepaymentBox: Interest Amount for repayment incorrect"
      )
    }

    "fund RepaymentBox has correct loan token value" in {
      client.getClient.execute { ctx =>
        val fundedValue: Long = 1000

        // Get repayment box as InputBox
        val inputRepaymentBox: InputBox = wrappedRepaymentBox.
          getAsInputBox(ctx, ctx.newTxBuilder(), dummyTxId, 0)

        // Create Fund Repayment Proxy Box
        val repaymentPaymentBox: SLTFundRepaymentProxyBox =
          new SLTFundRepaymentProxyBox(createFundRepaymentPaymentBox(
            repaymentBoxId = inputRepaymentBox.getId.getBytes,
            funderAddress = dummyAddress,
            sigUSDValue = fundedValue
          ))

        // Create Funded Repayment Input Box
        // by inserting the created inputRepaymentBox and the payment box
        val fundedSLTRepaymentInputBox: InputBox = SLTRepaymentBox
          .fundBox(new SLTRepaymentBox(inputRepaymentBox), repaymentPaymentBox)
          .getAsInputBox(ctx, ctx.newTxBuilder(), dummyTxId, 0)

        // Wrap the new funded repayment box
        val fundedWrappedSLTRepaymentBox: SLTRepaymentBox =
          new SLTRepaymentBox(fundedSLTRepaymentInputBox)

        checkLendBoxDetails(fundedWrappedSLTRepaymentBox)
        checkRepaymentDetails(fundedWrappedSLTRepaymentBox)
        assert(
          fundedWrappedSLTRepaymentBox.tokens(1).getId.equals(SigUSD.id),
          "SLTLendBox: Loan Token incorrect"
        )
        assert(
          fundedWrappedSLTRepaymentBox.tokens(1).getValue == fundedValue,
          "SLTLendBox: Funded Value incorrect"
        )
      }
    }
  }

  "SLTServiceBox" should {
    val wrappedServiceBox: SLTServiceBox = createGenesisServiceBox()

    "get accurate outbox" in {
      checkServiceBoxDetails(wrappedServiceBox)
    }

    "input box created is accurate" in {
      client.getClient.execute { ctx =>
        val sltInputBox: InputBox =
          wrappedServiceBox.getAsInputBox(ctx, ctx.newTxBuilder(), dummyTxId, 0)
        val wrappedSLTRepaymentInputBox: SLTServiceBox =
          new SLTServiceBox(sltInputBox)

        checkServiceBoxDetails(wrappedSLTRepaymentInputBox)
      }
    }

    def checkServiceBoxDetails(serviceBox: SLTServiceBox): Unit = {
      assert(
        serviceBox.exlePubKey.address
          .equals(serviceOwner.getErgoAddress.toString)
      )
      assert(
        serviceBox.profitSharingRegister.profitSharingPercentage == ServiceConfig.profitSharingPercentage
      )
      assert(
        serviceBox.profitSharingRegister.serviceFeeAmount == ServiceConfig.serviceFee
      )
    }

    "create lend token comparison" in {
      client.getClient.execute { ctx =>
        val createLendServiceInputBox: InputBox = SLTServiceBox
          .createLendBox(wrappedServiceBox)
          .getAsInputBox(ctx, ctx.newTxBuilder(), dummyTxId, 0)
        val createLendWrappedServiceBox: SLTServiceBox =
          new SLTServiceBox(createLendServiceInputBox)

        checkServiceBoxDetails(createLendWrappedServiceBox)
        assert(
          createLendWrappedServiceBox.tokens.head.getId
            .equals(SLTTokens.serviceNFTId),
          "SLTServiceBox: Service Token incorrect"
        )
        assert(
          createLendWrappedServiceBox.tokens.head.getValue == wrappedServiceBox.tokens.head.getValue,
          "SLTServiceBox: Service Token Value incorrect"
        )
        assert(
          createLendWrappedServiceBox
            .tokens(1)
            .getId
            .equals(SLTTokens.lendTokenId),
          "SLTServiceBox: Lend Token incorrect"
        )
        assert(
          createLendWrappedServiceBox
            .tokens(1)
            .getValue == wrappedServiceBox.tokens(1).getValue - 1,
          "SLTServiceBox: Lend Token Value incorrect"
        )
        assert(
          createLendWrappedServiceBox
            .tokens(2)
            .getId
            .equals(SLTTokens.repaymentTokenId),
          "SLTServiceBox: Repayment Token incorrect"
        )
        assert(
          createLendWrappedServiceBox
            .tokens(2)
            .getValue == wrappedServiceBox.tokens(2).getValue,
          "SLTServiceBox: Repayment Value incorrect"
        )
      }
    }

    "lend to repayment token comparison" in {
      client.getClient.execute { ctx =>
        val mutateLendServiceInputBox: InputBox = SLTServiceBox
          .mutateLendToRepaymentBox(wrappedServiceBox)
          .getAsInputBox(ctx, ctx.newTxBuilder(), dummyTxId, 0)
        val mutateLendWrappedServiceBox: SLTServiceBox =
          new SLTServiceBox(mutateLendServiceInputBox)

        checkServiceBoxDetails(mutateLendWrappedServiceBox)
        assert(
          mutateLendWrappedServiceBox.tokens.head.getId
            .equals(SLTTokens.serviceNFTId),
          "SLTServiceBox: Service Token incorrect"
        )
        assert(
          mutateLendWrappedServiceBox.tokens.head.getValue == wrappedServiceBox.tokens.head.getValue,
          "SLTServiceBox: Service Token Value incorrect"
        )
        assert(
          mutateLendWrappedServiceBox
            .tokens(1)
            .getId
            .equals(SLTTokens.lendTokenId),
          "SLTServiceBox: Lend Token incorrect"
        )
        assert(
          mutateLendWrappedServiceBox
            .tokens(1)
            .getValue == wrappedServiceBox.tokens(1).getValue + 1,
          "SLTServiceBox: Lend Token Value incorrect"
        )
        assert(
          mutateLendWrappedServiceBox
            .tokens(2)
            .getId
            .equals(SLTTokens.repaymentTokenId),
          "SLTServiceBox: Repayment Token incorrect"
        )
        assert(
          mutateLendWrappedServiceBox
            .tokens(2)
            .getValue == wrappedServiceBox.tokens(2).getValue - 1,
          "SLTServiceBox: Repayment Value incorrect"
        )
      }
    }

    "absorb repayment token comparison" in {
      client.getClient.execute { ctx =>
        val absorbRepaymentServiceInputBox: InputBox = SLTServiceBox
          .absorbRepaymentBox(wrappedServiceBox)
          .getAsInputBox(ctx, ctx.newTxBuilder(), dummyTxId, 0)
        val absorbRepaymentWrappedServiceBox: SLTServiceBox =
          new SLTServiceBox(absorbRepaymentServiceInputBox)

        checkServiceBoxDetails(absorbRepaymentWrappedServiceBox)
        assert(
          absorbRepaymentWrappedServiceBox.tokens.head.getId
            .equals(SLTTokens.serviceNFTId),
          "SLTServiceBox: Service Token incorrect"
        )
        assert(
          absorbRepaymentWrappedServiceBox.tokens.head.getValue == wrappedServiceBox.tokens.head.getValue,
          "SLTServiceBox: Service Token Value incorrect"
        )
        assert(
          absorbRepaymentWrappedServiceBox
            .tokens(1)
            .getId
            .equals(SLTTokens.lendTokenId),
          "SLTServiceBox: Lend Token incorrect"
        )
        assert(
          absorbRepaymentWrappedServiceBox
            .tokens(1)
            .getValue == wrappedServiceBox.tokens(1).getValue,
          "SLTServiceBox: Lend Token Value incorrect"
        )
        assert(
          absorbRepaymentWrappedServiceBox
            .tokens(2)
            .getId
            .equals(SLTTokens.repaymentTokenId),
          "SLTServiceBox: Repayment Token incorrect"
        )
        assert(
          absorbRepaymentWrappedServiceBox
            .tokens(2)
            .getValue == wrappedServiceBox.tokens(2).getValue - 1,
          "SLTServiceBox: Repayment Value incorrect"
        )
      }
    }

    "absorb lend token comparison" in {
      client.getClient.execute { ctx =>
        val absorbLendServiceInputBox: InputBox = SLTServiceBox
          .absorbLendBox(wrappedServiceBox)
          .getAsInputBox(ctx, ctx.newTxBuilder(), dummyTxId, 0)
        val absorbLendWrappedServiceBox: SLTServiceBox =
          new SLTServiceBox(absorbLendServiceInputBox)

        checkServiceBoxDetails(absorbLendWrappedServiceBox)
        assert(
          absorbLendWrappedServiceBox.tokens.head.getId
            .equals(SLTTokens.serviceNFTId),
          "SLTServiceBox: Service Token incorrect"
        )
        assert(
          absorbLendWrappedServiceBox.tokens.head.getValue == wrappedServiceBox.tokens.head.getValue,
          "SLTServiceBox: Service Token Value incorrect"
        )
        assert(
          absorbLendWrappedServiceBox
            .tokens(1)
            .getId
            .equals(SLTTokens.lendTokenId),
          "SLTServiceBox: Lend Token incorrect"
        )
        assert(
          absorbLendWrappedServiceBox
            .tokens(1)
            .getValue == wrappedServiceBox.tokens(1).getValue + 1,
          "SLTServiceBox: Lend Token Value incorrect"
        )
        assert(
          absorbLendWrappedServiceBox
            .tokens(2)
            .getId
            .equals(SLTTokens.repaymentTokenId),
          "SLTServiceBox: Repayment Token incorrect"
        )
        assert(
          absorbLendWrappedServiceBox
            .tokens(2)
            .getValue == wrappedServiceBox.tokens(2).getValue,
          "SLTServiceBox: Repayment Value incorrect"
        )
      }
    }
  }
}
