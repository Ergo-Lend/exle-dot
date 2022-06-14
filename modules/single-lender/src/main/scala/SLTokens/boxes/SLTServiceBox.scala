package SLTokens.boxes

import SLTokens.contracts.SLTLendBoxContract
import boxes.Box
import org.ergoplatform.appkit.{ErgoToken, ErgoType, InputBox}
import registers.{RegColl, RegVal}
import special.collection.Coll

case class SLTServiceBox(inputBox: InputBox, contractData: SLTLendBoxContract) extends Box(inputBox) {
  val serviceNFT: ErgoToken     = this.tokens.head
  val lendToken: ErgoToken      = this.tokens(1)
  val repaymentToken: ErgoToken = this.tokens(2)

  override def R4: RegVal[_] = {
    RegColl(inputBox.getRegisters.get(0).getValue.asInstanceOf[Coll[Long]].toArray, ErgoType.longType())
  }

  override def R5: RegVal[_] = {
    RegColl(inputBox.getRegisters.get(1).getValue.asInstanceOf[Coll[Coll[Byte]]].toArray, ErgoType.collType(ErgoType.byteType()))
  }

  override def R6: RegVal[_] = {
    RegColl(inputBox.getRegisters.get(2).getValue.asInstanceOf[Coll[Byte]].toArray, ErgoType.byteType())
  }

  override def R7: RegVal[_] = {
    RegColl(inputBox.getRegisters.get(3).getValue.asInstanceOf[Coll[Byte]].toArray, ErgoType.byteType())
  }

  override def R8: RegVal[_] = {
    RegColl(inputBox.getRegisters.get(4).getValue.asInstanceOf[Coll[Long]].toArray, ErgoType.longType())
  }

  def creationInfo: RegColl[Long]         = this.R4.asInstanceOf[RegColl[Long]]
  def serviceBoxInfo: RegColl[Coll[Byte]] = this.R5.asInstanceOf[RegColl[Coll[Byte]]]
  def boxVersion: RegColl[Byte]           = this.R6.asInstanceOf[RegColl[Byte]]
  def exlePubKey: RegColl[Byte]           = this.R7.asInstanceOf[RegColl[Byte]]
  def profitSharePercent: RegColl[Long]   = this.R8.asInstanceOf[RegColl[Long]]
}
