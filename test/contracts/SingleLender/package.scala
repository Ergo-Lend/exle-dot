package contracts

import boxes.registers.RegisterTypes.StringRegister
import config.Configs
import features.lend.boxes.{LendServiceBox, SingleLenderLendBox, SingleLenderRepaymentBox}
import features.lend.boxes.registers.{BorrowerRegister, CreationInfoRegister, FundingInfoRegister, LendingProjectDetailsRegister, ProfitSharingRegister, RepaymentDetailsRegister, ServiceBoxInfoRegister, SingleAddressRegister, SingleLenderRegister}
import org.ergoplatform.appkit.{Address, Parameters}

/**
 * We need
 * 1. Service Boxes
 * 2. Lend Boxes
 * 3. Repayment Boxes
 */
package object SingleLender {
  val goal: Long = 1e9.toLong
  val interestRate = 100
  val repaymentHeightLength = 100
  val deadlineHeightLength = 100
  val loanName = "Test Loan"
  val loanDescription = "Test Loan Description"

  def buildGenesisServiceBox(): LendServiceBox = {
    val creationInfo = new CreationInfoRegister(creationHeight = 1L)
    val serviceInfo = new ServiceBoxInfoRegister(name = "LendBox", description = "Testing")
    val boxInfo = new StringRegister("SingleLenderServiceBox")
    val ownerPubKey = new SingleAddressRegister(Configs.serviceOwner.toString)
    val profitSharingRegister = new ProfitSharingRegister(Configs.profitSharingPercentage, Configs.serviceFee)
    val lendServiceBox = new LendServiceBox(
      value = Parameters.MinFee,
      lendTokenAmount = 100,
      repaymentTokenAmount = 100,
      creationInfo = creationInfo,
      serviceInfo = serviceInfo,
      boxInfo = boxInfo,
      ergoLendPubKey = ownerPubKey,
      profitSharingPercentage = profitSharingRegister
    )

    lendServiceBox
  }

  def createWrappedLendBox(value: Long = Parameters.MinFee,
                           goal: Long = goal,
                           deadlineHeightLength: Long = deadlineHeightLength,
                           interestRate: Long = interestRate,
                           repaymentHeightLength: Long = repaymentHeightLength,
                           loanName: String = loanName,
                           loanDescription: String = loanDescription,
                           borrowerAddress: Address = dummyAddress): SingleLenderLendBox = {
    ergoClient.execute {
      ctx => {
        val fundingInfoRegister = new FundingInfoRegister(
          fundingGoal = goal,
          deadlineHeight = ctx.getHeight + deadlineHeightLength,
          interestRatePercent = interestRate,
          repaymentHeightLength = repaymentHeightLength
        )
        val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(
          projectName = loanName,
          description = loanDescription,
        )
        val borrowerRegister = new BorrowerRegister(borrowerAddress.toString)
        val wrappedInputLendBox = new SingleLenderLendBox(
          value = value,
          fundingInfoRegister = fundingInfoRegister,
          lendingProjectDetailsRegister = lendingProjectDetailsRegister,
          borrowerRegister = borrowerRegister,
          singleLenderRegister = SingleLenderRegister.emptyRegister
        )

        wrappedInputLendBox
      }
    }
  }

  def createWrappedRepaymentBox(fundingGoal: Long = goal,
                                interestRate: Long = interestRate,
                                lendersAddress: Address = dummyAddress): SingleLenderRepaymentBox = {
    ergoClient.execute {
      ctx => {
        val wrappedLendBox = createWrappedLendBox(goal = fundingGoal, interestRate = interestRate).fundBox(lendersAddress.toString)
        val wrappedRepaymentBox = new SingleLenderRepaymentBox(wrappedLendBox, ctx.getHeight + 100)

        wrappedRepaymentBox
      }
    }
  }

  def createRawWrappedRepaymentBox(value: Long = Parameters.MinFee,
                                   goal: Long = goal,
                                   deadlineHeightLength: Long = deadlineHeightLength,
                                   interestRate: Long = interestRate,
                                   repaymentHeightLength: Long = repaymentHeightLength,
                                   loanName: String = loanName,
                                   loanDescription: String = loanDescription,
                                   borrowerAddress: Address = dummyAddress,
                                   lenderAddress: Address = dummyAddress,
                                   fundedRepaymentHeight: Long): SingleLenderRepaymentBox = {
    ergoClient.execute {
      ctx => {
        val fundingInfoRegister = new FundingInfoRegister(
          fundingGoal = goal,
          deadlineHeight = ctx.getHeight + deadlineHeightLength,
          interestRatePercent = interestRate,
          repaymentHeightLength = repaymentHeightLength
        )
        val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(
          projectName = loanName,
          description = loanDescription,
        )
        val borrowerRegister = new BorrowerRegister(borrowerAddress.toString)
        val lenderRegister = new SingleLenderRegister(lenderAddress.toString)

        val repaymentDetailsRegister = RepaymentDetailsRegister.apply(fundedRepaymentHeight, fundingInfoRegister)

        val wrappedInputRepaymentBox = new SingleLenderRepaymentBox(
          value = value,
          fundingInfoRegister = fundingInfoRegister,
          lendingProjectDetailsRegister = lendingProjectDetailsRegister,
          borrowerRegister = borrowerRegister,
          singleLenderRegister = lenderRegister,
          repaymentDetailsRegister = repaymentDetailsRegister
        )

        wrappedInputRepaymentBox
      }
    }
  }
}
