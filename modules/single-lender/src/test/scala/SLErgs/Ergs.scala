import SLErgs.boxes.{SLELendBox, SLERepaymentBox, SLEServiceBox}
import SLErgs.contracts.SLELendBoxContract
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
import commons.boxes.registers.RegisterTypes.StringRegister
import commons.configs.ServiceConfig
import commons.node.Client
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{
  Address,
  ErgoProver,
  ErgoToken,
  InputBox,
  NetworkType,
  OutBox,
  Parameters
}

/**
  * We need
  * 1. Service Boxes
  * 2. Lend Boxes
  * 3. Repayment Boxes
  */
package object SLErgs {
  val goal: Long = 1e9.toLong
  val interestRate: Long = 100L
  val repaymentHeightLength: Long = 100L
  val deadlineHeightLength: Long = 100L
  val loanName = "Test Loan"
  val loanDescription = "Test Loan Description"
  val client: Client = new Client()
  val dummyAddress: Address = Address.create("4MQyML64GnzMxZgm")

  val dummyTxId =
    "ce552663312afc2379a91f803c93e2b10b424f176fbc930055c10def2fd88a5d"

  val dummyToken =
    "f5cc03963b64d3542b8cea49d5436666a97f6a2d098b7d3b2220e824b5a91819"
  val networkType = NetworkType.TESTNET

  def dummyProver: ErgoProver =
    client.getClient.execute { ctx =>
      val prover = ctx
        .newProverBuilder()
        .withDLogSecret(BigInt.apply(0).bigInteger)
        .build()

      return prover
    }

  def buildUserBox(value: Long, index: Short = 0): InputBox =
    client.getClient.execute { ctx =>
      val inputBox = ctx
        .newTxBuilder()
        .outBoxBuilder()
        .value(value)
        .contract(
          new ErgoTreeContract(dummyAddress.getErgoAddress.script, networkType)
        )
        .build()
        .convertToInputWith(dummyTxId, index)

      return inputBox
    }

  def buildGenesisServiceBox(): SLEServiceBox = {
    val creationInfo = new CreationInfoRegister(creationHeight = 1L)
    val serviceInfo =
      new ServiceBoxInfoRegister(name = "LendBox", description = "Testing")
    val boxInfo = new StringRegister("SLEServiceBox")
    val ownerPubKey = new SingleAddressRegister(
      ServiceConfig.serviceOwner.toString
    )
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
      val lendToken: ErgoToken = new ErgoToken(LendServiceTokens.lendTokenId, 1)

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
