package ergo

import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoContract, NetworkType}
import scorex.crypto.hash.Digest32

object ContractUtils {
  def getContractScriptHash(contract: ErgoContract): Digest32 = {
    scorex.crypto.hash.Blake2b256(contract.getErgoTree.bytes)
  }

  /**
   * Alternative:
   *
   * return ctx.compileContract(
   * ConstantsBuilder.create()
   * .item("recipientPk", recipient.getPublicKey())
   * .build(),
   * "{ recipientPk }")
   *
   * @param ctx
   * @param recipient
   * @return
   */
  def sendToPK(recipient: Address, networkType: NetworkType = NetworkType.MAINNET): ErgoContract = {
    val contract = new ErgoTreeContract(recipient.getErgoAddress.script, networkType)

    contract
  }
}
