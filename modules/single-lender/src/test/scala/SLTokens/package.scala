import SLErgs.{
  client,
  deadlineHeightLength,
  dummyAddress,
  goal,
  interestRate,
  loanDescription,
  loanName,
  repaymentHeightLength
}
import SLTokens.boxes.{SLTLendBox, SLTRepaymentBox, SLTServiceBox}
import commons.boxes.registers.RegisterTypes.StringRegister
import commons.configs.{ServiceConfig, Tokens}
import commons.registers.{
  BorrowerRegister,
  CreationInfoRegister,
  FundingInfoRegister,
  LendingProjectDetailsRegister,
  ProfitSharingRegister,
  RepaymentDetailsRegister,
  ServiceBoxInfoRegister,
  SingleAddressRegister,
  SingleLenderRegister
}
import org.ergoplatform.appkit.{Address, ErgoToken, Parameters}

package object SLTokens {
  client.setClient()

  def createGenesisServiceBox(): SLTServiceBox = {
    val creationInfo = CreationInfoRegister(creationHeight = 1L)
    val serviceInfo =
      ServiceBoxInfoRegister(name = "LendBox", description = "Testing")
    val boxInfo = new StringRegister("SLEServiceBox")
    val ownerPubKey = new SingleAddressRegister(
      ServiceConfig.serviceOwner.toString
    )
    val profitSharingRegister = new ProfitSharingRegister(
      ServiceConfig.profitSharingPercentage,
      ServiceConfig.serviceFee
    )
    val lendServiceBox = new SLTServiceBox(
      value = Parameters.MinFee,
      creationInfoRegister = creationInfo,
      serviceBoxInfoRegister = serviceInfo,
      boxInfo = boxInfo,
      exlePubKey = ownerPubKey,
      profitSharingRegister = profitSharingRegister
    )

    lendServiceBox
  }

  def createWrappedSLTLendBox(
    value: Long = Parameters.MinFee,
    goal: Long = goal,
    deadlineHeightLength: Long = deadlineHeightLength,
    interestRate: Long = interestRate,
    repaymentHeightLength: Long = repaymentHeightLength,
    loanName: String = loanName,
    loanDescription: String = loanDescription,
    borrowerAddress: Address = dummyAddress,
    sigUSDAmount: Long = 0,
    lendTokenAmount: Long = 1,
    lenderAddress: Address = null
  ): SLTLendBox =
    client.getClient.execute { ctx =>
      val lendToken: ErgoToken =
        new ErgoToken(SLTTokens.lendTokenId, lendTokenAmount)
      val sigUSD: ErgoToken = new ErgoToken(Tokens.sigUSD, sigUSDAmount)
      val tokens: Seq[ErgoToken] =
        if (sigUSDAmount != 0) Seq(lendToken, sigUSD) else Seq(lendToken)
      val fundingInfoRegister = FundingInfoRegister(
        fundingGoal = goal,
        deadlineHeight = ctx.getHeight + deadlineHeightLength,
        interestRatePercent = interestRate,
        repaymentHeightLength = repaymentHeightLength,
        creationHeight = client.getHeight
      )
      val lendingProjectDetailsRegister = LendingProjectDetailsRegister(
        projectName = loanName,
        description = loanDescription
      )
      val borrowerRegister = new BorrowerRegister(borrowerAddress.toString)
      val loanTokenIdRegister = new StringRegister(Tokens.sigUSD)
      val wrappedInputLendBox = SLTLendBox(
        value = value,
        fundingInfoRegister = fundingInfoRegister,
        lendingProjectDetailsRegister = lendingProjectDetailsRegister,
        borrowerRegister = borrowerRegister,
        loanTokenIdRegister = loanTokenIdRegister,
        singleLenderRegister =
          if (lenderAddress == null) SingleLenderRegister.emptyRegister
          else new SingleLenderRegister(lenderAddress.toString),
        tokens = tokens
      )

      wrappedInputLendBox
    }

  def createWrappedSLTRepaymentBox(
    value: Long = Parameters.MinFee,
    goal: Long = goal,
    deadlineHeightLength: Long = deadlineHeightLength,
    interestRate: Long = interestRate,
    repaymentHeightLength: Long = repaymentHeightLength,
    loanName: String = loanName,
    loanDescription: String = loanDescription,
    borrowerAddress: Address = dummyAddress,
    lenderAddress: Address = dummyAddress
  ): SLTRepaymentBox =
    client.getClient.execute { ctx =>
      val fundingInfoRegister = FundingInfoRegister(
        fundingGoal = goal,
        deadlineHeight = ctx.getHeight + deadlineHeightLength,
        interestRatePercent = interestRate,
        repaymentHeightLength = repaymentHeightLength,
        creationHeight = client.getHeight
      )
      val lendingProjectDetailsRegister = LendingProjectDetailsRegister(
        projectName = loanName,
        description = loanDescription
      )
      val borrowerRegister = new BorrowerRegister(borrowerAddress.toString)
      val lenderRegister = new SingleLenderRegister(lenderAddress.toString)

      val repaymentDetailsRegister = RepaymentDetailsRegister.apply(
        fundedHeight = ctx.getHeight + deadlineHeightLength - 10,
        fundingInfoRegister
      )
      val loanTokenIdRegister = new StringRegister(Tokens.sigUSD)

      val wrappedInputRepaymentBox = SLTRepaymentBox(
        value = value,
        fundingInfoRegister = fundingInfoRegister,
        lendingProjectDetailsRegister = lendingProjectDetailsRegister,
        borrowerRegister = borrowerRegister,
        loanTokenIdRegister = loanTokenIdRegister,
        singleLenderRegister = lenderRegister,
        repaymentDetailsRegister = repaymentDetailsRegister
      )

      wrappedInputRepaymentBox
    }
}
