package SLTokens.boxes

import SLTokens.SLTTokens
import boxes.{Box, BoxWrapper}
import commons.boxes.registers.RegisterTypes.CollByteRegister
import commons.contracts.ExleContracts
import commons.ergo.ErgCommons
import commons.registers.SingleLenderRegister
import contracts.Contract
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoContract, ErgoId, ErgoToken, OutBox, UnsignedTransactionBuilder}

class SLTFundLendProxyBox(
  override val value: Long,
  boxIdRegister: CollByteRegister,
  lenderRegister: SingleLenderRegister,
  override val tokens: Seq[ErgoToken] = Seq(),
  override val id: ErgoId = ErgoId.create(""),
  override val box: Option[Box] = Option(null)
                         )
    extends BoxWrapper {

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
      .tokens(tokens: _*)
      .registers(
        boxIdRegister.toRegister,
        lenderRegister.toRegister)
      .contract(this.getContract(ctx))
      .build()
  }

  override def getContract(ctx: BlockchainContext): ErgoContract = {
    val constants: List[(String, Any)] = List(
      (SLTFundLendProxyBoxConstants.boxIdToFund, (new ErgoId(boxIdRegister.value).getBytes)),
      (SLTFundLendProxyBoxConstants.minFee, ErgCommons.MinMinerFee),
      (SLTFundLendProxyBoxConstants.lenderPk, Address.create(lenderRegister.address).getErgoAddress.script.bytes),
      (SLTFundLendProxyBoxConstants.sltLendTokenId, SLTTokens.lendTokenId.getBytes)
    )

    Contract.build(
      ExleContracts.SLTFundLendBoxProxyContract.contractScript,
      constants = constants: _*
    )(ctx).ergoContract
  }
}

object SLTFundLendProxyBoxConstants {
  val boxIdToFund: String = "_BoxIdToFund"
  val lenderPk: String = "_LenderPk"
  val minFee: String = "_MinFee"
  val sltLendTokenId: String = "_SLTLendTokenId"
}

object SLTFundLendProxyBox {
  def getBox(
    boxId: String,
    lenderAddress: Address,
    tokens: Seq[ErgoToken],
    value: Long
            ): SLTFundLendProxyBox = {
    val lenderRegister: SingleLenderRegister = new SingleLenderRegister(lenderAddress)
    val boxIdRegister: CollByteRegister = new CollByteRegister(ErgoId.create(boxId).getBytes)

    new SLTFundLendProxyBox(
      value = value,
      tokens = tokens,
      lenderRegister = lenderRegister,
      boxIdRegister = boxIdRegister
    )
  }
}