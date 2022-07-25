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
import boxes.{Box, BoxWrapper, FundsToAddressBox}
import commons.boxes.registers.RegisterTypes.CollByteRegister
import commons.configs.ServiceConfig
import commons.ergo.ErgCommons
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
  loanTokenIdRegister: CollByteRegister,
  singleLenderRegister: SingleLenderRegister,
  override val id: ErgoId = ErgoId.create(""),
  override val tokens: Seq[ErgoToken] = Seq(
    new ErgoToken(SLTTokens.lendTokenId, 1)
  ),
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
    singleLenderRegister =
      if (inputBox.getRegisters.size() > 4)
        new SingleLenderRegister(
          inputBox.getRegisters.get(4).getValue.asInstanceOf[Coll[Byte]].toArray
        )
      else
        SingleLenderRegister.emptyRegister,
    value = inputBox.getValue,
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
          // Add single lender register
          singleLenderRegister.toRegister
        )
      }

    addRegistersBoxBuilder.build()
  }
}

object SLTLendBox {

  def getFunded(sltLendBox: SLTLendBox, lenderAddress: Address): SLTLendBox = {
    val token: ErgoToken = SigUSD(sltLendBox.fundingInfoRegister.fundingGoal).toErgoToken
    val fundedSLTLendBox: SLTLendBox =
      sltLendBox.copy(
        singleLenderRegister = new SingleLenderRegister(lenderAddress),
        tokens = sltLendBox.tokens ++ Seq(token)
      )

    fundedSLTLendBox
  }

  def getFunded(
    sltLendBox: SLTLendBox,
    paymentBox: SLTFundLendProxyBox
  ): SLTLendBox = {
    val lenderAddressRegister: SingleLenderRegister = paymentBox.lenderRegister
    val lenderAddress: Address =
      Address.create(lenderAddressRegister.lendersAddress)

    getFunded(sltLendBox, lenderAddress)
  }

  def fromCreatePaymentBox(paymentInputBox: SLTCreateLendProxyBox): SLTLendBox =
    new SLTLendBox(paymentInputBox.box.get.input).copy(
      value =
        paymentInputBox.value - ServiceConfig.serviceFee - ErgCommons.MinMinerFee,
      tokens = Seq(new ErgoToken(SLTTokens.lendTokenId, 1))
    )

  def getBorrowerFundedBox(sltLendBox: SLTLendBox): FundsToAddressBox = {
    val tokenId: ErgoId = new ErgoId(sltLendBox.loanTokenIdRegister.value)
    FundsToAddressBox(
      Address.create(sltLendBox.borrowerRegister.address),
      tokens = sltLendBox.tokens.filter(_.getId.equals(tokenId))
    )
  }
}
