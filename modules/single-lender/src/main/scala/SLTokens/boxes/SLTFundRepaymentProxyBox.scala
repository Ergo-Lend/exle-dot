package SLTokens.boxes

import SLTokens.SLTTokens
import boxes.{Box, BoxWrapper}
import commons.boxes.registers.RegisterTypes.{AddressRegister, CollByteRegister}
import commons.contracts.ExleContracts
import commons.ergo.ErgCommons
import contracts.Contract
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoContract, ErgoId, ErgoToken, InputBox, OutBox, UnsignedTransactionBuilder}

class SLTFundRepaymentProxyBox(
  override val value: Long,
  boxIdRegister: CollByteRegister,
  fundersAddress: AddressRegister,
  override val tokens: Seq[ErgoToken] = Seq.empty,
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null)
) extends BoxWrapper {
  /**
   * Get Outbox returns the immediate Outbox of the wrapper.
   * This means it does not go through any modification
   *
   * @param ctx Blockchain Context
   * @param txB TxBuilder
   * @return
   */
  override def getOutBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    txB.outBoxBuilder()
      .value(value)
      .contract(this.getContract(ctx))
      .tokens(tokens: _*)
      .registers(
        boxIdRegister.toRegister,
        fundersAddress.toRegister
      )
      .build()
  }

  override def getContract(ctx: BlockchainContext): ErgoContract = {
    val constants: List[(String, Any)] = List(
      (SLTFundRepaymentProxyBoxConstants.boxIdToFund, boxIdRegister.value),
      (SLTFundRepaymentProxyBoxConstants.funderPk, Address.create(fundersAddress.address).getErgoAddress.script.bytes),
      (SLTFundRepaymentProxyBoxConstants.minFee, ErgCommons.MinMinerFee),
      (SLTFundRepaymentProxyBoxConstants.sltRepaymentTokenId, SLTTokens.repaymentTokenId.getBytes)
    )

    Contract.build(
      ExleContracts.SLTFundRepaymentBoxProxyContract.contractScript,
      constants = constants: _*
    )(ctx).ergoContract
  }
}

object SLTFundRepaymentProxyBoxConstants {
  val boxIdToFund: String = "_BoxIdToFund"
  val funderPk: String = "_FunderPk"
  val minFee: String = "_MinFee"
  val sltRepaymentTokenId: String = "_SLTRepaymentTokenId"
}

object SLTFundRepaymentProxyBox {
  def getBox(boxId: String,
             fundersAddress: Address,
             tokens: Seq[ErgoToken],
             value: Long): SLTFundRepaymentProxyBox = {
    val fundersRegister: AddressRegister = new AddressRegister(fundersAddress.toString)
    val boxIdRegister: CollByteRegister = new CollByteRegister(ErgoId.create(boxId).getBytes)

    new SLTFundRepaymentProxyBox(
      value = value,
      tokens = tokens,
      fundersAddress = fundersRegister,
      boxIdRegister = boxIdRegister)
  }
}