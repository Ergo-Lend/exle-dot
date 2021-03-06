package commons.registers

import commons.boxes.registers.RegisterTypes.AddressRegister
import org.ergoplatform.appkit.Address

trait LenderRegister

/**
  * SingleLenderRegister
  * @param lendersAddress Pubkey of Lender
  */
final case class SingleLenderRegister(lendersAddress: String)
    extends AddressRegister(lendersAddress)
    with LenderRegister {
  def this(registerData: Array[Byte]) = this(
    lendersAddress = AddressRegister.getAddress(registerData).toString
  )

  def this(address: Address) = this(
    lendersAddress = address.getErgoAddress.toString
  )
}

final class BorrowerRegister(val borrowersAddress: String)
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
