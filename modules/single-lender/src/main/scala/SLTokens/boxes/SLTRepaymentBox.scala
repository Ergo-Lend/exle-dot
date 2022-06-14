package SLTokens.boxes

import SLTokens.contracts.{SLTLendBoxContract, SLTRepaymentBoxContract}
import boxes.Box
import org.ergoplatform.appkit.{ErgoToken, ErgoType, InputBox}
import registers.{RegColl, RegVal}
import special.collection.Coll

case class SLTRepaymentBox(inputBox: InputBox, contractData: SLTRepaymentBoxContract) extends Box(inputBox) {
  val repaymentToken: ErgoToken = this.tokens.head
  val loanToken: ErgoToken = this.tokens(1)

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
    RegColl(inputBox.getRegisters.get(4).getValue.asInstanceOf[Coll[Byte]].toArray, ErgoType.byteType())
  }

  override def R9: RegVal[_] = {
    RegColl(inputBox.getRegisters.get(5).getValue.asInstanceOf[Coll[Long]].toArray, ErgoType.longType())
  }

  def fundingInfo: RegColl[Long]          = this.R4.asInstanceOf[RegColl[Long]]
  def projectDetails: RegColl[Coll[Byte]] = this.R5.asInstanceOf[RegColl[Coll[Byte]]]
  def borrowerPK: RegColl[Byte]           = this.R6.asInstanceOf[RegColl[Byte]]
  def loanTokenType: RegColl[Byte]        = this.R7.asInstanceOf[RegColl[Byte]]
  def singleLenderPK: RegColl[Byte]       = this.R8.asInstanceOf[RegColl[Byte]]
  def repaymentDetails: RegColl[Long]     = this.R9.asInstanceOf[RegColl[Long]]
}
