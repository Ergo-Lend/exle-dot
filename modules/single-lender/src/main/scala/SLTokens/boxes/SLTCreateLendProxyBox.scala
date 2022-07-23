package SLTokens.boxes

import SLTokens.SLTTokens
import boxes.{Box, BoxWrapper}
import commons.boxes.registers.RegisterTypes.CollByteRegister
import commons.configs.Configs
import commons.contracts.ExleContracts
import commons.ergo.ErgCommons
import commons.registers.{BorrowerRegister, FundingInfoRegister, LendingProjectDetailsRegister}
import contracts.Contract
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoContract, ErgoId, ErgoToken, OutBox, UnsignedTransactionBuilder}

class SLTCreateLendProxyBox(
                             override val value: Long,
                             fundingInfoRegister: FundingInfoRegister,
                             lendingProjectDetailsRegister: LendingProjectDetailsRegister,
                             borrowerRegister: BorrowerRegister,
                             loanTokenIdRegister: CollByteRegister,
                             override val tokens: Seq[ErgoToken] = Seq(),
                             override val id: ErgoId = ErgoId.create(""),
                             override val box: Option[Box] = Option(null),
) extends BoxWrapper {
  /**
   * Get Outbox returns the immediate Outbox of the wrapper.
   * This means it does not go through any modification
   *
   * @param ctx Blockchain Context
   * @param txB TxBuilder
   * @return
   */
  override def getOutBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
     txB
       .outBoxBuilder()
       .value(value)
       .contract(this.getContract(ctx))
       .tokens(tokens: _*)
       .registers(
         fundingInfoRegister.toRegister,
         lendingProjectDetailsRegister.toRegister,
         borrowerRegister.toRegister,
         loanTokenIdRegister.toRegister
       )
       .build()
  }

  // Get Contract from the registers.
  // use Exle-generics. Contract
  override def getContract(ctx: BlockchainContext): ErgoContract =
  {
    val constants: List[(String, Any)] = List(
      (CreateLendProxyContractConstants.borrowerPk, Address.create(borrowerRegister.borrowersAddress).getErgoAddress.script.bytes),
      (CreateLendProxyContractConstants.loanTokenId, loanTokenIdRegister.value),
      (CreateLendProxyContractConstants.minFee, ErgCommons.MinMinerFee),
      (CreateLendProxyContractConstants.refundHeightThreshold, ctx.getHeight + ((Configs.creationDelay / 60 / 2) + 1)),
      (CreateLendProxyContractConstants.goal, fundingInfoRegister.fundingGoal),
      (CreateLendProxyContractConstants.deadlineHeight, fundingInfoRegister.deadlineHeight),
      (CreateLendProxyContractConstants.interestRate, fundingInfoRegister.interestRatePercent),
      (CreateLendProxyContractConstants.repaymentHeightLength, fundingInfoRegister.repaymentHeightLength),
      (CreateLendProxyContractConstants.sltServiceNftId, SLTTokens.serviceNFTId.getBytes),
      (CreateLendProxyContractConstants.sltLendTokenId, SLTTokens.lendTokenId.getBytes)
    )

    Contract.build(
      ExleContracts.SLTCreateLendBoxProxyContract.contractScript,
      constants = constants: _*
    )(ctx).ergoContract
  }
}

object CreateLendProxyContractConstants {
  val borrowerPk: String = "_BorrowerPk"
  val loanTokenId: String = "_LoanTokenId"
  val minFee: String = "_MinFee"
  val refundHeightThreshold: String = "_RefundHeightThreshold"
  val goal: String = "_Goal"
  val deadlineHeight: String = "_DeadlineHeight"
  val interestRate: String = "_InterestRate"
  val repaymentHeightLength: String = "_RepaymentHeightLength"
  val sltServiceNftId: String = "_SLTServiceNFTId"
  val sltLendTokenId: String = "_SLTLendTokenId"
}

// GET Call (Not post cause we don't save to DB no more)
// Contracts
// Proxy Out Box
// Tx
// Reduced Tx
// ErgoPay Response
object SLTCreateLendProxyBox {
  def getBox(value: Long,
              borrowerPk: String,
             loanToken: Array[Byte],
             projectName: String,
             description: String,
             deadlineHeight: Long,
             goal: Long,
             interestRate: Long,
             repaymentHeightLength: Long)(implicit ctx: BlockchainContext): SLTCreateLendProxyBox = {
    val fundingInfoRegister: FundingInfoRegister = FundingInfoRegister(
      fundingGoal = goal,
      deadlineHeight = ctx.getHeight + deadlineHeight,
      interestRatePercent = interestRate,
      repaymentHeightLength = repaymentHeightLength,
      creationHeight = ctx.getHeight
    )

    val lendingProjectDetailsRegister = LendingProjectDetailsRegister(
      projectName = projectName,
      description = description
    )

    val borrowerRegister = new BorrowerRegister(borrowerPk)
    val loanTokenIdRegister = new CollByteRegister(loanToken)

    new SLTCreateLendProxyBox(
      value = value,
      fundingInfoRegister = fundingInfoRegister,
      lendingProjectDetailsRegister = lendingProjectDetailsRegister,
      borrowerRegister = borrowerRegister,
      loanTokenIdRegister = loanTokenIdRegister
    )
  }
}
