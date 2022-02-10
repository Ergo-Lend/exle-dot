package ergotools

import org.ergoplatform.appkit.{Address, BlockchainContext, ConstantsBuilder, ErgoContract}
import scorex.crypto.hash.Digest32

object ContractUtils {
  def getContractScriptHash(contract: ErgoContract): Digest32 = {
    scorex.crypto.hash.Blake2b256(contract.getErgoTree.bytes)
  }

  def sendToPK(ctx: BlockchainContext, recipient: Address): ErgoContract = {
    return ctx.compileContract(
      ConstantsBuilder.create()
        .item("recipientPk", recipient.getPublicKey())
        .build(),
      "{ recipientPk }")
  }
}
