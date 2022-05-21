package contracts.SingleLender

import boxes.registers.RegisterTypes.StringRegister
import configs.ServiceConfig
import contracts._
import core.SingleLender.Ergs.boxes.registers._
import core.SingleLender.Ergs.boxes.{SLELendBox, SLERepaymentBox, SLEServiceBox}
import core.tokens.LendServiceTokens
import org.ergoplatform.appkit.{Address, ErgoToken, OutBox, Parameters}

/**
  * We need
  * 1. Service Boxes
  * 2. Lend Boxes
  * 3. Repayment Boxes
  */
package object Ergs {
  val goal: Long = 1e9.toLong
  val interestRate: Long = 100L
  val repaymentHeightLength: Long = 100L
  val deadlineHeightLength: Long = 100L
  val loanName = "Test Loan"
  val loanDescription = "Test Loan Description"

  def buildGenesisServiceBox(): SLEServiceBox = {
    val creationInfo = new CreationInfoRegister(creationHeight = 1L)
    val serviceInfo =
      new ServiceBoxInfoRegister(name = "LendBox", description = "Testing")
    val boxInfo = new StringRegister("SLEServiceBox")
    val ownerPubKey = new SingleAddressRegister(ServiceConfig.serviceOwner.toString)
    val profitSharingRegister = new ProfitSharingRegister(
      ServiceConfig.profitSharingPercentage,
      ServiceConfig.serviceFee
    )
    val lendServiceBox = new SLEServiceBox(
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

  def createWrappedLendBox(
    value: Long = Parameters.MinFee,
    goal: Long = goal,
    deadlineHeightLength: Long = deadlineHeightLength,
    interestRate: Long = interestRate,
    repaymentHeightLength: Long = repaymentHeightLength,
    loanName: String = loanName,
    loanDescription: String = loanDescription,
    borrowerAddress: Address = dummyAddress,
    lenderAddress: Address = null
  ): SLELendBox =
    client.getClient.execute { ctx =>
      val fundingInfoRegister = new FundingInfoRegister(
        fundingGoal = goal,
        deadlineHeight = ctx.getHeight + deadlineHeightLength,
        interestRatePercent = interestRate,
        repaymentHeightLength = repaymentHeightLength,
        creationHeight = client.getHeight
      )
      val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(
        projectName = loanName,
        description = loanDescription
      )
      val borrowerRegister = new BorrowerRegister(borrowerAddress.toString)
      val wrappedInputLendBox = new SLELendBox(
        value = value,
        fundingInfoRegister = fundingInfoRegister,
        lendingProjectDetailsRegister = lendingProjectDetailsRegister,
        borrowerRegister = borrowerRegister,
        singleLenderRegister =
          if (lenderAddress == null) SingleLenderRegister.emptyRegister
          else new SingleLenderRegister(lenderAddress.toString)
      )

      wrappedInputLendBox
    }

  def createOutputLendBox(
    value: Long = Parameters.MinFee,
    goal: Long = goal,
    deadlineHeightLength: Long = deadlineHeightLength,
    interestRate: Long = interestRate,
    repaymentHeightLength: Long = repaymentHeightLength,
    loanName: String = loanName,
    loanDescription: String = loanDescription,
    borrowerAddress: Address = dummyAddress,
    lenderAddress: Address = null
  ): OutBox =
    client.getClient.execute { ctx =>
      val txB = ctx.newTxBuilder()
      val fundingInfoRegister = new FundingInfoRegister(
        fundingGoal = goal,
        deadlineHeight = ctx.getHeight + deadlineHeightLength,
        interestRatePercent = interestRate,
        repaymentHeightLength = repaymentHeightLength,
        creationHeight = client.getHeight
      )
      val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(
        projectName = loanName,
        description = loanDescription
      )
      val borrowerRegister = new BorrowerRegister(borrowerAddress.toString)

      val lendBoxContract = SLELendBoxContract.getContract(ctx)
      val lendToken: ErgoToken = new ErgoToken(LendServiceTokens.lendToken, 1)

      val lendBox = txB
        .outBoxBuilder()
        .value(value)
        .contract(lendBoxContract)
        .tokens(lendToken)
        .registers(
          fundingInfoRegister.toRegister,
          lendingProjectDetailsRegister.toRegister,
          borrowerRegister.toRegister,
          if (lenderAddress == null)
            SingleLenderRegister.emptyRegister.toRegister
          else
            new SingleLenderRegister(lenderAddress.toString).toRegister
        )
        .build()

      lendBox
    }

  def createWrappedRepaymentBox(
    fundingGoal: Long = goal,
    interestRate: Long = interestRate,
    lendersAddress: Address = dummyAddress
  ): SLERepaymentBox =
    client.getClient.execute { ctx =>
      val wrappedLendBox =
        createWrappedLendBox(goal = fundingGoal, interestRate = interestRate)
          .fundBox(lendersAddress.toString)
      val wrappedRepaymentBox =
        new SLERepaymentBox(wrappedLendBox, (ctx.getHeight + 100).toLong)

      wrappedRepaymentBox
    }

  def createRawWrappedRepaymentBox(
    value: Long = Parameters.MinFee,
    goal: Long = goal,
    deadlineHeightLength: Long = deadlineHeightLength,
    interestRate: Long = interestRate,
    repaymentHeightLength: Long = repaymentHeightLength,
    loanName: String = loanName,
    loanDescription: String = loanDescription,
    borrowerAddress: Address = dummyAddress,
    lenderAddress: Address = dummyAddress,
    fundedRepaymentHeight: Long
  ): SLERepaymentBox =
    client.getClient.execute { ctx =>
      val fundingInfoRegister = new FundingInfoRegister(
        fundingGoal = goal,
        deadlineHeight = ctx.getHeight + deadlineHeightLength,
        interestRatePercent = interestRate,
        repaymentHeightLength = repaymentHeightLength,
        creationHeight = client.getHeight
      )
      val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(
        projectName = loanName,
        description = loanDescription
      )
      val borrowerRegister = new BorrowerRegister(borrowerAddress.toString)
      val lenderRegister = new SingleLenderRegister(lenderAddress.toString)

      val repaymentDetailsRegister = RepaymentDetailsRegister.apply(
        fundedRepaymentHeight,
        fundingInfoRegister
      )

      val wrappedInputRepaymentBox = new SLERepaymentBox(
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
