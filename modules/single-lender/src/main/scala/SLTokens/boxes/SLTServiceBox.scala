package SLTokens.boxes

import SLTokens.SLTTokens
import SLTokens.contracts.SLTServiceBoxContract
import boxes.{Box, BoxWrapper}
import commons.boxes.registers.RegisterTypes.StringRegister
import commons.registers.{
  CreationInfoRegister,
  ProfitSharingRegister,
  ServiceBoxInfoRegister,
  SingleAddressRegister
}
import org.ergoplatform.appkit.{
  BlockchainContext,
  ErgoContract,
  ErgoId,
  ErgoToken,
  InputBox,
  OutBox,
  UnsignedTransactionBuilder
}
import special.collection.Coll
import tokens.TokenHelper

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

case class SLTServiceBox(
  value: Long,
  creationInfoRegister: CreationInfoRegister,
  serviceBoxInfoRegister: ServiceBoxInfoRegister,
  boxInfo: StringRegister,
  exlePubKey: SingleAddressRegister,
  profitSharingRegister: ProfitSharingRegister,
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null),
  override val tokens: Seq[ErgoToken] = Seq(
    new ErgoToken(SLTTokens.serviceNFTId, 1),
    new ErgoToken(SLTTokens.lendTokenId, 1000000000),
    new ErgoToken(SLTTokens.repaymentTokenId, 1000000000)
  )
) extends BoxWrapper {
  def this(inputBox: InputBox) = this(
    value = inputBox.getValue,
    creationInfoRegister = new CreationInfoRegister(
      inputBox.getRegisters.get(0).getValue.asInstanceOf[Coll[Long]].toArray
    ),
    serviceBoxInfoRegister = new ServiceBoxInfoRegister(
      inputBox.getRegisters
        .get(1)
        .getValue
        .asInstanceOf[Coll[Coll[Byte]]]
        .toArray
    ),
    boxInfo = new StringRegister(
      inputBox.getRegisters.get(2).getValue.asInstanceOf[Coll[Byte]]
    ),
    exlePubKey = new SingleAddressRegister(
      inputBox.getRegisters.get(3).getValue.asInstanceOf[Coll[Byte]].toArray
    ),
    profitSharingRegister = new ProfitSharingRegister(
      inputBox.getRegisters.get(4).getValue.asInstanceOf[Coll[Long]].toArray
    ),
    id = inputBox.getId,
    tokens = inputBox.getTokens.asScala.toSeq,
    box = Option(Box(inputBox))
  )

  override def getContract(ctx: BlockchainContext): ErgoContract =
    SLTServiceBoxContract.getContract(ctx).contract.ergoContract

  override def getOutBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder
  ): OutBox =
    txB
      .outBoxBuilder()
      .value(value)
      .contract(getContract(ctx))
      .tokens(tokens: _*)
      .registers(
        creationInfoRegister.toRegister,
        serviceBoxInfoRegister.toRegister,
        boxInfo.toRegister,
        exlePubKey.toRegister,
        profitSharingRegister.toRegister
      )
      .build()
}

object SLTServiceBox {

  def createLendBox(sltServiceBox: SLTServiceBox): SLTServiceBox = {
    val newTokenList: Seq[ErgoToken] =
      sltServiceBox.tokens.map(TokenHelper.increment(_, SLTTokens.lendTokenId))
    sltServiceBox.copy(tokens = newTokenList)
  }

  def mutateLendToRepaymentBox(sltServiceBox: SLTServiceBox): SLTServiceBox = {
    val newTokenList: Seq[ErgoToken] = sltServiceBox.tokens
      .map(TokenHelper.increment(_, SLTTokens.lendTokenId))
      .map(TokenHelper.decrement(_, SLTTokens.repaymentTokenId))

    sltServiceBox.copy(tokens = newTokenList)
  }

  def absorbLendBox(sltServiceBox: SLTServiceBox): SLTServiceBox =
    sltServiceBox.copy(tokens =
      sltServiceBox.tokens.map(TokenHelper.increment(_, SLTTokens.lendTokenId))
    )

  def absorbRepaymentBox(sltServiceBox: SLTServiceBox): SLTServiceBox =
    sltServiceBox.copy(tokens =
      sltServiceBox.tokens.map(
        TokenHelper.decrement(_, SLTTokens.repaymentTokenId)
      )
    )
}
