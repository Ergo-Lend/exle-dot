package SLErgs.boxes

import commons.boxes.Box
import commons.ergo.ContractUtils
import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  OutBox,
  UnsignedTransactionBuilder
}

/**
  * Funds to Address
  * When a repayment box or lending box is fully funded. The spent output will consists of boxes
  * to the respective addresses (lender or borrower)
  */
case class FundsToAddressBox(val value: Long, val address: Address) {
  def this(value: Long, address: String) = this(
    value = value,
    address = Address.create(address)
  )

  def getOutputBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder
  ): OutBox = {
    val outputBox = txB
      .outBoxBuilder()
      .value(value)
      .contract(ContractUtils.sendToPK(address))
      .build()

    outputBox
  }
}

abstract class addressBox() extends Box {
  def getAddress: ErgoAddress

  def getOutputBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder
  ): OutBox
}
