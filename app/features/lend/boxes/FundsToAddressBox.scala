package features.lend.boxes

import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{Address, BlockchainContext, OutBox, UnsignedTransactionBuilder}

/**
 * Funds to Address
 * When a repayment box or lending box is fully funded. The spent output will consists of boxes
 * to the respective addresses (lender or borrower)
 */
case class FundsToAddressBox(val value: Long, val ergoAddress: ErgoAddress) {
  def this(value: Long, address: String) = this(
    value = value,
    ergoAddress = Address.create(address).getErgoAddress
  )

  def getAddress: ErgoAddress = {
    ergoAddress
  }

  def getOutputBox(txB: UnsignedTransactionBuilder): OutBox = {
    val outputBox = txB.outBoxBuilder()
      .value(value)
      .contract(new ErgoTreeContract(getAddress.script))
      .build()

    outputBox
  }
}

abstract class addressBox() extends Box {
  def getAddress: ErgoAddress
  def getOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox
}