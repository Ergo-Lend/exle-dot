package features.lend.boxes.registers

import boxes.registers.RegisterTypes.{CollByte, StringRegister}

trait LenderRegister

/**
 * SingleLenderRegister
 * @param lendersAddress Pubkey of Lender
 */
class SingleLenderRegister(val lendersAddress: String) extends StringRegister(lendersAddress) with LenderRegister {
  def this(registerData: Array[Byte]) = this(
    lendersAddress = CollByte.arrayByteToString(registerData)
  )
}

class SingleAddressRegister(val address: String) extends StringRegister(address) {
  def this(registerData: Array[Byte]) = this(
    address = CollByte.arrayByteToString(registerData)
  )
}

object SingleLenderRegister {
  def emptyRegister: SingleLenderRegister = {
    new SingleLenderRegister(Array.emptyByteArray)
  }
}