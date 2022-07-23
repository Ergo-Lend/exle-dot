package boxes

import commons.ergo.{ContractUtils, ErgCommons}
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoContract,
  ErgoId,
  ErgoToken,
  InputBox,
  NetworkType,
  OutBox,
  UnsignedTransactionBuilder
}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

case class FundsToAddressBox(
  address: Address,
  value: Long = ErgCommons.MinBoxFee,
  override val id: ErgoId = ErgoId.create(""),
  override val tokens: Seq[ErgoToken] = Seq.empty,
  override val box: Option[Box] = Option(null)
) extends BoxWrapper {

  def this(inputBox: InputBox) = this(
    address = Address.fromErgoTree(inputBox.getErgoTree, NetworkType.MAINNET),
    value = inputBox.getValue,
    id = inputBox.getId,
    tokens = inputBox.getTokens.asScala.toSeq,
    box = Option(Box(inputBox))
  )

  /**
    * Get Outbox returns the immediate Outbox of the wrapper.
    * This means it does not go through any modification
    *
    * @param ctx Blockchain Context
    * @param txB TxBuilder
    * @return
    */
  override def getOutBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder
  ): OutBox =
    if (tokens.isEmpty) {
      txB
        .outBoxBuilder()
        .value(value)
        .contract(getContract(ctx))
        .build()
    } else {
      txB
        .outBoxBuilder()
        .value(value)
        .tokens(tokens: _*)
        .contract(getContract(ctx))
        .build()
    }

  override def getContract(ctx: BlockchainContext): ErgoContract =
    ContractUtils.sendToPK(address)
}
