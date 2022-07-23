import SLErgs.{
  client,
  deadlineHeightLength,
  dummyAddress,
  dummyTxId,
  goal,
  interestRate,
  loanDescription,
  loanName,
  repaymentHeightLength
}
import SLTokens.boxes.{SLTLendBox, SLTRepaymentBox, SLTServiceBox}
import SLTokens.contracts.{SLTCreateLendBoxContract, SLTProxyContractService}
import commons.boxes.registers.RegisterTypes.{CollByteRegister, StringRegister}
import commons.configs.{ServiceConfig, Tokens}
import commons.ergo.ErgCommons
import commons.registers.{
  BorrowerRegister,
  CreationInfoRegister,
  FundingInfoRegister,
  LendingProjectDetailsRegister,
  ProfitSharingRegister,
  RepaymentDetailsRegisterV2,
  ServiceBoxInfoRegister,
  SingleAddressRegister,
  SingleLenderRegister
}
import org.ergoplatform.appkit.{
  Address,
  ErgoToken,
  InputBox,
  OutBoxBuilder,
  Parameters,
  UnsignedTransactionBuilder
}
import tokens.SigUSD

package object SLTokens {
  client.setClient()

  val sltProxyContractService: SLTProxyContractService =
    new SLTProxyContractService(client)

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
      val tokens: Seq[ErgoToken] = {
        if (sigUSDAmount != 0) Seq(lendToken, sigUSD) else Seq(lendToken)
      }
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
      val loanTokenIdRegister = new CollByteRegister(SigUSD.id.getBytes)
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
    repaymentTokenAmount: Long = 1,
    sigUSDAmount: Long = 0,
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

      val repaymentDetailsRegister = RepaymentDetailsRegisterV2.apply(
        fundedHeight = ctx.getHeight + deadlineHeightLength - 10,
        fundingInfoRegister
      )
      val loanTokenIdRegister = new CollByteRegister(SigUSD.id.getBytes)

      val lendToken: ErgoToken =
        new ErgoToken(SLTTokens.repaymentTokenId, repaymentTokenAmount)
      val sigUSD: ErgoToken = new ErgoToken(Tokens.sigUSD, sigUSDAmount)
      val tokens: Seq[ErgoToken] = {
        if (sigUSDAmount != 0) Seq(lendToken, sigUSD) else Seq(lendToken)
      }

      val wrappedInputRepaymentBox = SLTRepaymentBox(
        value = value,
        fundingInfoRegister = fundingInfoRegister,
        lendingProjectDetailsRegister = lendingProjectDetailsRegister,
        borrowerRegister = borrowerRegister,
        loanTokenIdRegister = loanTokenIdRegister,
        singleLenderRegister = lenderRegister,
        repaymentDetailsRegister = repaymentDetailsRegister,
        tokens = tokens
      )

      wrappedInputRepaymentBox
    }

  def createInitiationPaymentBox(
    value: Long = ServiceConfig.serviceFee + (ErgCommons.MinMinerFee * 2),
    goal: Long = goal,
    deadlineHeightLength: Long = deadlineHeightLength,
    interestRate: Long = interestRate,
    repaymentHeightLength: Long = repaymentHeightLength,
    loanName: String = loanName,
    loanDescription: String = loanDescription,
    borrowerAddress: Address = dummyAddress
  ): InputBox =
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
      val loanTokenIdRegister = new CollByteRegister(SigUSD.id.getBytes)

      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
      val contractValueBoxBuilder: OutBoxBuilder = txB
        .outBoxBuilder()
        .contract(
          sltProxyContractService.getSLTLendCreateProxyContract(
            borrowerPk = borrowerAddress.getErgoAddress.toString,
            loanToken = SigUSD.id.getBytes,
            ctx.getHeight + deadlineHeightLength,
            goal = goal,
            interestRate = interestRate,
            repaymentHeightLength = repaymentHeightLength
          )
        )
        .value(value)

      contractValueBoxBuilder.registers(
        fundingInfoRegister.toRegister,
        lendingProjectDetailsRegister.toRegister,
        borrowerRegister.toRegister,
        loanTokenIdRegister.toRegister
      )

      contractValueBoxBuilder.build().convertToInputWith(dummyTxId, 0)
    }

  def createFundLendPaymentBox(
    lendBoxId: String,
    value: Long = ServiceConfig.serviceFee + (ErgCommons.MinMinerFee * 2),
    lenderAddress: Address = dummyAddress
  ): InputBox =
    client.getClient.execute { ctx =>
      val lenderRegister = new SingleLenderRegister(lenderAddress.toString)
      val sigUSD: ErgoToken = new ErgoToken(Tokens.sigUSD, value)

      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
      val contractValueBoxBuilder: OutBoxBuilder = txB
        .outBoxBuilder()
        .contract(
          sltProxyContractService.getSLTFundLendBoxProxyContract(
            lendBoxId = lendBoxId,
            lenderAddress = lenderAddress.toString
          )
        )
        .value(Parameters.MinFee)
        .tokens(sigUSD)

      contractValueBoxBuilder.registers(
        lenderRegister.toRegister
      )

      contractValueBoxBuilder.build().convertToInputWith(dummyTxId, 0)
    }

  def createFundRepaymentPaymentBox(
    repaymentBoxId: String,
    sigUSDValue: Long,
    value: Long = ErgCommons.MinMinerFee * 4,
    funderAddress: Address = dummyAddress
  ): InputBox =
    client.getClient.execute { ctx =>
      val sigUSD: ErgoToken = new ErgoToken(Tokens.sigUSD, sigUSDValue)

      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
      val contractValueBoxBuilder: OutBoxBuilder = txB
        .outBoxBuilder()
        .contract(
          sltProxyContractService.getSLTFundRepaymentBoxProxyContract(
            repaymentBoxId = repaymentBoxId,
            funderAddress = funderAddress.toString
          )
        )
        .value(value)
        .tokens(sigUSD)

      contractValueBoxBuilder.build().convertToInputWith(dummyTxId, 0)
    }
}
