package SLTokens.boxes

import SLTokens.SLTTokens
import boxes.{Box, BoxWrapper}
import commons.boxes.registers.RegisterTypes.{AddressRegister, CollByteRegister}
import commons.contracts.ExleContracts
import commons.ergo.ErgCommons
import commons.registers.SingleLenderRegister
import contracts.Contract
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoContract, ErgoId, ErgoToken, InputBox, OutBox, UnsignedTransactionBuilder}
import special.collection.Coll

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class SLTFundRepaymentProxyBox(
  override val value: Long,
  boxIdRegister: CollByteRegister,
  fundersAddress: AddressRegister,
  override val tokens: Seq[ErgoToken] = Seq.empty,
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null)
) extends BoxWrapper {

  def this(inputBox: InputBox) = this(
    value = inputBox.getValue,
    tokens = inputBox.getTokens.asScala.toSeq,
    id = inputBox.getId,
    box = Option(Box(inputBox)),
    boxIdRegister = new CollByteRegister(inputBox.getRegisters.get(0).getValue.asInstanceOf[Coll[Byte]].toArray),
    fundersAddress = new SingleLenderRegister(inputBox.getRegisters.get(1).getValue.asInstanceOf[Coll[Byte]].toArray)
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
  ): OutBox =
    txB
      .outBoxBuilder()
      .value(value)
      .contract(this.getContract(ctx))
      .tokens(tokens: _*)
      .registers(
        boxIdRegister.toRegister,
        fundersAddress.toRegister
      )
      .build()

  override def getContract(ctx: BlockchainContext): ErgoContract = {
    val constants: List[(String, Any)] = List(
      (SLTFundRepaymentProxyBoxConstants.minFee, ErgCommons.MinMinerFee),
      (
        SLTFundRepaymentProxyBoxConstants.sltRepaymentTokenId,
        SLTTokens.repaymentTokenId.getBytes
      )
    )

    Contract
      .build(
        ExleContracts.SLTFundRepaymentBoxProxyContract.contractScript,
        constants = constants: _*
      )(ctx)
      .ergoContract
  }
}

object SLTFundRepaymentProxyBoxConstants {
  val minFee: String = "_MinFee"
  val sltRepaymentTokenId: String = "_SLTRepaymentTokenId"
}

object SLTFundRepaymentProxyBox {

  def getBox(
    boxId: Array[Byte],
    fundersAddress: Address,
    tokens: Seq[ErgoToken],
    value: Long
  ): SLTFundRepaymentProxyBox = {
    val fundersRegister: AddressRegister = new AddressRegister(
      fundersAddress.toString
    )
    val boxIdRegister: CollByteRegister = new CollByteRegister(
      boxId
    )

    new SLTFundRepaymentProxyBox(
      value = value,
      tokens = tokens,
      fundersAddress = fundersRegister,
      boxIdRegister = boxIdRegister
    )
  }
}
