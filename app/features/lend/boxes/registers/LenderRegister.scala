package features.lend.boxes.registers

import boxes.registers.RegisterTypes.CollByteRegister
import org.ergoplatform.appkit.ErgoValue
import special.collection.Coll

trait LenderRegister

/**
 * SingleLenderRegister
 * @param lendersAddress Pubkey of Lender
 */
class SingleLenderRegister(lendersAddress: String) extends CollByteRegister with LenderRegister {
  def this(registerData: Array[Byte]) = this(
    lendersAddress = arrayByteToString(registerData)
  )

  def toRegister: ErgoValue[Coll[Byte]] = {
    val register = stringToCollByte(lendersAddress)

    ergoValueOf(register)
  }
}

object SingleLenderRegister {
  def emptyRegister: SingleLenderRegister = {
    new SingleLenderRegister(Array.emptyByteArray)
  }
}