package SLTokens.contracts

import commons.contracts.ExleContracts
import contracts.Contract
import org.ergoplatform.appkit.{BlockchainContext, ErgoId}
import sigmastate.eval.Colls

case class SLTServiceBoxContract(contract: Contract,
                                 minFee: Long, ownerPK: Array[Byte], lendBoxHash: Array[Byte],
                                 repaymentBoxHash: Array[Byte]){
}

object SLTServiceBoxContract {
  def build(minFee: Long, ownerPK: Array[Byte], lendBoxHash: Array[Byte],
            repaymentBoxHash: Array[Byte])(implicit ctx: BlockchainContext): SLTServiceBoxContract = {

    SLTServiceBoxContract(
      Contract.build(ExleContracts.SLTServiceBoxGuardScript.contractScript,
        "_MinFee" -> minFee,
        "_OwnerPK" -> Colls.fromArray(ownerPK),
        "_SLTLendBoxHash" -> Colls.fromArray(lendBoxHash),
        "_SLTRepaymentBoxHash" -> Colls.fromArray(repaymentBoxHash),
      ),
      minFee, ownerPK, lendBoxHash, repaymentBoxHash)
  }
}
