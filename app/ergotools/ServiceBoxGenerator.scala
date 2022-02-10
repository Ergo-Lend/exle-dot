package ergotools

import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoToken, InputBox, Parameters, UnsignedTransaction, UnsignedTransactionBuilder}

import scala.collection.JavaConverters.seqAsJavaListConverter

class ServiceBoxGenerator {

}

/**
 * Contains the service NFT
 */
class ServiceNFTBox(
                    val tokenName: String,
                    val tokenDesc: String
                   ) {
  def createServiceNFTBox(
                           inputBox: InputBox,
                           address: Address,
                           ctx: BlockchainContext,
                           txB: UnsignedTransactionBuilder): UnsignedTransaction = {

    val token = new ErgoToken(inputBox.getId, 1)
    val serviceNFTBox = txB.outBoxBuilder()
      .value(2)
      .mintToken(token, tokenName, tokenDesc, 0)
      .contract(ContractUtils.sendToPK(ctx, address))
      .build()

    val boxesToSpend = Seq(inputBox)
    val tx = txB
      .boxesToSpend(boxesToSpend.asJava)
      .outputs(serviceNFTBox)
      .fee(Parameters.MinFee)
      .sendChangeTo(address.asP2PK())
      .build()

    tx
  }
}

/**
 * Contains the tokens NFT
 */
class ServiceTokensBox(
                        val tokenName: String,
                        val tokenDesc: String
                      ) {
  def createTokensBox(
                           inputBox: InputBox,
                           address: Address,
                           ctx: BlockchainContext,
                           txB: UnsignedTransactionBuilder): UnsignedTransaction = {

    val token = new ErgoToken(inputBox.getId, 1000000000000L)
    val serviceNFTBox = txB.outBoxBuilder()
      .value(2)
      .mintToken(token, tokenName, tokenDesc, 0)
      .contract(ContractUtils.sendToPK(ctx, address))
      .build()

    val boxesToSpend = Seq(inputBox)
    val tx = txB
      .boxesToSpend(boxesToSpend.asJava)
      .outputs(serviceNFTBox)
      .fee(Parameters.MinFee)
      .sendChangeTo(address.asP2PK())
      .build()

    tx
  }
}