package boxes

import contracts.Contract
import org.ergoplatform.appkit._
import registers.RegVal
import sigmastate.Values

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

/**
  * A pure data model and InputBox wrapper, representing a box on Ergo
  * @param input InputBox to wrap
  */
case class Box(input: InputBox) {

  def R4: RegVal[_] = RegVal(input.getRegisters.get(0).getValue)
  def R5: RegVal[_] = RegVal(input.getRegisters.get(1).getValue)
  def R6: RegVal[_] = RegVal(input.getRegisters.get(2).getValue)
  def R7: RegVal[_] = RegVal(input.getRegisters.get(3).getValue)
  def R8: RegVal[_] = RegVal(input.getRegisters.get(4).getValue)
  def R9: RegVal[_] = RegVal(input.getRegisters.get(5).getValue)

  def tokens: Seq[ErgoToken] = input.getTokens.asScala.toSeq
  def value: Long = input.getValue.longValue()
  def id: ErgoId = input.getId
  def bytes: Array[Byte] = input.getBytes
  def ergoTree: Values.ErgoTree = input.getErgoTree

  def contract(implicit ctx: BlockchainContext): Contract =
    Contract.fromErgoTree(input.getErgoTree)

  def getErgValue: Double =
    (BigDecimal(value) / Parameters.OneErg).doubleValue()
}

object Box {

  /**
    * Create a boxes.Box from an OutBox by converting to an InputBox and wrapping
    * @param output OutBox to convert
    * @param txId Transaction id used to convert output
    * @param index Output index used to convert output
    * @return A boxes.Box wrapping the converted output
    */
  def ofOutBox(output: OutBox, txId: String, index: Int): Box =
    Box(output.convertToInputWith(txId, index.shortValue()))
}

abstract class BoxWrapper {
  val id: ErgoId
  val box: Option[Box]
  val tokens: Seq[ErgoToken]
  val value: Long

  /**
    * Get Outbox returns the immediate Outbox of the wrapper.
    * This means it does not go through any modification
    * @param ctx Blockchain Context
    * @param txB TxBuilder
    * @return
    */
  def getOutBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox
  def getContract(ctx: BlockchainContext): ErgoContract

  def toInputBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder,
    txId: String,
    index: Int
  ): InputBox =
    getOutBox(ctx, txB).convertToInputWith(txId, index.shortValue())
}
