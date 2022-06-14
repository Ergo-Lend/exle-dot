package SLTokens.contracts

import commons.contracts.ExleContracts
import contracts.Contract
import org.ergoplatform.appkit.{BlockchainContext, ErgoContract, ErgoId}
import sigmastate.eval.Colls

case class SLTLendBoxContract(contract: Contract,
                              minFee: Long, minBoxAmnt: Long, serviceBoxNFTId: ErgoId,
                              lendTokenId: ErgoId, repaymentTokenId: ErgoId){
}

object SLTLendBoxContract {
  def build(minFee: Long, minBoxAmnt: Long, serviceBoxNFTId: ErgoId,
            lendTokenId: ErgoId, repaymentTokenId: ErgoId)(implicit ctx: BlockchainContext): SLTLendBoxContract = {

    SLTLendBoxContract(
      Contract.build(ExleContracts.SLTLendBoxGuardScript.contractScript,
        "_MinFee" -> minFee,
        "_MinBoxAmount" -> minBoxAmnt,
        "_SLTServiceBoxNFTId" -> Colls.fromArray(serviceBoxNFTId.getBytes),
        "_SLTLendTokenId" -> Colls.fromArray(lendTokenId.getBytes),
        "_SLTRepaymentTokenId" -> Colls.fromArray(repaymentTokenId.getBytes)
      ),
      minFee, minBoxAmnt, serviceBoxNFTId, lendTokenId, repaymentTokenId)
  }
}
