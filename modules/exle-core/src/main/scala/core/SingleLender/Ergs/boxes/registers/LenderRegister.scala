package core.SingleLender.Ergs.boxes.registers

import boxes.registers.RegisterTypes.{AddressRegister, CollByte, StringRegister}

trait LenderRegister

/**
  * SingleLenderRegister
  * @param lendersAddress Pubkey of Lender
  */
class SingleLenderRegister(val lendersAddress: String)
    extends AddressRegister(lendersAddress)
    with LenderRegister {
  def this(registerData: Array[Byte]) = this(
    lendersAddress = AddressRegister.getAddress(registerData).toString
  )
}

class BorrowerRegister(val borrowersAddress: String)
    extends SingleAddressRegister(borrowersAddress) {
  def this(registerData: Array[Byte]) = this(
    borrowersAddress = AddressRegister.getAddress(registerData).toString
  )
}

class SingleAddressRegister(override val address: String)
    extends AddressRegister(address) {
  def this(registerData: Array[Byte]) = this(
    address = AddressRegister.getAddress(registerData).toString
  )
}

object SingleLenderRegister {

  def emptyRegister: SingleLenderRegister =
    new SingleLenderRegister("")
}
