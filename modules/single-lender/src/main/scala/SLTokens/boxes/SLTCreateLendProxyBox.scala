package SLTokens.boxes

import SLTokens.{SLTTokens, TxFeeCosts}
import boxes.{Box, BoxWrapper}
import commons.boxes.registers.RegisterTypes.CollByteRegister
import commons.configs.ServiceConfig
import commons.contracts.ExleContracts
import commons.ergo.ErgCommons
import commons.registers.{
  BorrowerRegister,
  FundingInfoRegister,
  LendingProjectDetailsRegister
}
import contracts.Contract
import org.ergoplatform.appkit.{
  BlockchainContext,
  ErgoContract,
  ErgoId,
  ErgoToken,
  InputBox,
  OutBox,
  OutBoxBuilder,
  UnsignedTransactionBuilder
}
import special.collection.Coll

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class SLTCreateLendProxyBox(
  override val value: Long,
  val fundingInfoRegister: FundingInfoRegister,
  val lendingProjectDetailsRegister: LendingProjectDetailsRegister,
  val borrowerRegister: BorrowerRegister,
  val loanTokenIdRegister: CollByteRegister,
  override val tokens: Seq[ErgoToken] = Seq(),
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null)
) extends BoxWrapper {
  def this(inputBox: InputBox) = this(
    fundingInfoRegister = new FundingInfoRegister(
      inputBox.getRegisters.get(0).getValue.asInstanceOf[Coll[Long]].toArray
    ),
    lendingProjectDetailsRegister = new LendingProjectDetailsRegister(
      inputBox.getRegisters
        .get(1)
        .getValue
        .asInstanceOf[Coll[Coll[Byte]]]
        .toArray
    ),
    borrowerRegister = new BorrowerRegister(
      inputBox.getRegisters.get(2).getValue.asInstanceOf[Coll[Byte]].toArray
    ),
    loanTokenIdRegister = new CollByteRegister(
      inputBox.getRegisters.get(3).getValue.asInstanceOf[Coll[Byte]].toArray
    ),
    value = inputBox.getValue,
    id = inputBox.getId,
    tokens = inputBox.getTokens.asScala.toSeq,
    box = Option(Box(inputBox))
  )

  /**
    * Get Outbox returns the immediate Outbox of the wrapper.
    * This means it does not go through any modification
    *
    * @param ctx Blockchain Context
    * @param txB TxBuilder
    * @return
    */
  override def getOutBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder
  ): OutBox = {
    val outBoxBuilder: OutBoxBuilder = txB
      .outBoxBuilder()
      .value(value)
      .contract(this.getContract(ctx))
      .registers(
        fundingInfoRegister.toRegister,
        lendingProjectDetailsRegister.toRegister,
        borrowerRegister.toRegister,
        loanTokenIdRegister.toRegister
      )

    val outBoxBuilderWithTokenCheck: OutBoxBuilder =
      if (tokens.nonEmpty)
        outBoxBuilder.tokens(tokens: _*)
      else
        outBoxBuilder

    outBoxBuilderWithTokenCheck.build()
  }

  // Get Contract from the registers.
  // use Exle-generics. Contract
  override def getContract(ctx: BlockchainContext): ErgoContract = {
    val constants: List[(String, Any)] = List(
      (CreateLendProxyContractConstants.minFee, ErgCommons.MinMinerFee),
      (
        CreateLendProxyContractConstants.sltServiceNftId,
        SLTTokens.serviceNFTId.getBytes
      ),
      (
        CreateLendProxyContractConstants.sltLendTokenId,
        SLTTokens.lendTokenId.getBytes
      )
    )

    Contract
      .build(
        ExleContracts.SLTCreateLendBoxProxyContract.contractScript,
        constants = constants: _*
      )(ctx)
      .ergoContract
  }
}

object CreateLendProxyContractConstants {
  val minFee: String = "_MinFee"
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

  def getBox(
    borrowerPk: String,
    loanToken: Array[Byte],
    projectName: String,
    description: String,
    deadlineHeight: Long,
    goal: Long,
    interestRate: Long,
    repaymentHeightLength: Long,
    value: Long = TxFeeCosts.creationTxFee
  )(implicit ctx: BlockchainContext): SLTCreateLendProxyBox = {
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
