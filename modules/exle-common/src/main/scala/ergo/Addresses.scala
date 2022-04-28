package ergo

import org.ergoplatform.appkit.ErgoContract
import scorex.crypto.hash.Digest32

object Addresses {
  def getContractScriptHash(contract: ErgoContract): Digest32 = {
    scorex.crypto.hash.Blake2b256(contract.getErgoTree.bytes)
  }
}
