package SLTokens.boxes

import SLTokens.SLTTokens
import boxes.{Box, BoxWrapper}
import commons.boxes.registers.RegisterTypes.CollByteRegister
import commons.contracts.ExleContracts
import commons.ergo.ErgCommons
import commons.registers.SingleLenderRegister
import contracts.Contract
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoContract, ErgoId, ErgoToken, InputBox, OutBox, UnsignedTransactionBuilder}
import special.collection.Coll

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class SLTFundLendProxyBox(
  override val value: Long,
  val boxIdRegister: CollByteRegister,
  val lenderRegister: SingleLenderRegister,
  override val tokens: Seq[ErgoToken] = Seq(),
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null)
) extends BoxWrapper {

  def this(inputBox: InputBox) = this(
    value = inputBox.getValue,
    tokens = inputBox.getTokens.asScala.toSeq,
    id = inputBox.getId,
    box = Option(Box(inputBox)),
    boxIdRegister = new CollByteRegister(inputBox.getRegisters.get(0).getValue.asInstanceOf[Coll[Byte]].toArray),
    lenderRegister = new SingleLenderRegister(inputBox.getRegisters.get(1).getValue.asInstanceOf[Coll[Byte]].toArray)
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
      .tokens(tokens: _*)
      .registers(boxIdRegister.toRegister, lenderRegister.toRegister)
      .contract(this.getContract(ctx))
      .build()

  override def getContract(ctx: BlockchainContext): ErgoContract = {
    val constants: List[(String, Any)] = List(
      (SLTFundLendProxyBoxConstants.minFee, ErgCommons.MinMinerFee),
      (
        SLTFundLendProxyBoxConstants.sltLendTokenId,
        SLTTokens.lendTokenId.getBytes
      )
    )

    Contract
      .build(
        ExleContracts.SLTFundLendBoxProxyContract.contractScript,
        constants = constants: _*
      )(ctx)
      .ergoContract
  }
}

object SLTFundLendProxyBoxConstants {
  val minFee: String = "_MinFee"
  val sltLendTokenId: String = "_SLTLendTokenId"
}

object SLTFundLendProxyBox {

  def getBox(
    boxId: Array[Byte],
    lenderAddress: Address,
    tokens: Seq[ErgoToken],
    value: Long
  ): SLTFundLendProxyBox = {
    val lenderRegister: SingleLenderRegister = new SingleLenderRegister(
      lenderAddress
    )
    val boxIdRegister: CollByteRegister = new CollByteRegister(
      boxId
    )

    new SLTFundLendProxyBox(
      value = value,
      tokens = tokens,
      lenderRegister = lenderRegister,
      boxIdRegister = boxIdRegister
    )
  }
}
