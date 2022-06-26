package SLTokens.boxes

import commons.registers.{
  BorrowerRegister,
  FundingInfoRegister,
  LendingProjectDetailsRegister,
  RepaymentDetailsRegister,
  SingleLenderRegister
}
import SLTokens.SLTTokens
import SLTokens.contracts.SLTLendBoxContract
import boxes.{Box, BoxWrapper}
import commons.boxes.registers.RegisterTypes.StringRegister
import org.ergoplatform.appkit.{
  Address,
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
import tokens.SigUSD

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

case class SLTLendBox(
  value: Long,
  fundingInfoRegister: FundingInfoRegister,
  lendingProjectDetailsRegister: LendingProjectDetailsRegister,
  borrowerRegister: BorrowerRegister,
  loanTokenIdRegister: StringRegister,
  singleLenderRegister: SingleLenderRegister,
  override val id: ErgoId = ErgoId.create(""),
  override val tokens: Seq[ErgoToken] = Seq(
    new ErgoToken(SLTTokens.lendTokenId, 1)
  ),
  override val box: Option[Box] = Option(null)
) extends BoxWrapper {
  def this(inputBox: InputBox) = this(
    value = inputBox.getValue,
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
    loanTokenIdRegister = new StringRegister(
      inputBox.getRegisters.get(3).getValue.asInstanceOf[Coll[Byte]].toArray
    ),
    singleLenderRegister =
      if (inputBox.getRegisters.size() > 4)
        new SingleLenderRegister(
          inputBox.getRegisters.get(4).getValue.asInstanceOf[Coll[Byte]].toArray
        )
      else
        SingleLenderRegister.emptyRegister,
    id = inputBox.getId,
    tokens = inputBox.getTokens.asScala.toSeq,
    box = Option(Box(inputBox))
  )

  override def getContract(ctx: BlockchainContext): ErgoContract =
    SLTLendBoxContract.getContract(ctx: BlockchainContext).contract.ergoContract

  override def getOutBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder
  ): OutBox = {
    val contractValueBoxBuilder: OutBoxBuilder = txB
      .outBoxBuilder()
      .value(value)
      .contract(this.getContract(ctx))
      .tokens(tokens: _*)

    val addRegistersBoxBuilder =
      if (singleLenderRegister.isEmpty) {
        contractValueBoxBuilder.registers(
          fundingInfoRegister.toRegister,
          lendingProjectDetailsRegister.toRegister,
          borrowerRegister.toRegister,
          loanTokenIdRegister.toRegister
        )
      } else {
        contractValueBoxBuilder.registers(
          fundingInfoRegister.toRegister,
          lendingProjectDetailsRegister.toRegister,
          borrowerRegister.toRegister,
          loanTokenIdRegister.toRegister,
          singleLenderRegister.toRegister
        )
      }

    addRegistersBoxBuilder.build()
  }
}

object SLTLendBox {

  def getFunded(sltLendBox: SLTLendBox, lenderAddress: Address): SLTLendBox = {
    val fundedSLTLendBox: SLTLendBox =
      sltLendBox.copy(
        singleLenderRegister = new SingleLenderRegister(lenderAddress),
        tokens = sltLendBox.tokens ++ Seq(
          SigUSD(sltLendBox.fundingInfoRegister.fundingGoal).toErgoToken
        )
      )

    fundedSLTLendBox
  }
}
