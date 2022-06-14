package SLTokens.contracts

import commons.contracts.ExleContracts
import contracts.Contract
import org.ergoplatform.appkit.{BlockchainContext, ErgoId}
import sigmastate.eval.Colls

case class SLTFundRepaymentBoxContract(contract: Contract,
                                       boxIdToFund: ErgoId, funderPK: Array[Byte],
                                       minFee: Long, repaymentTokenId: ErgoId)

object SLTFundRepaymentBoxContract {
  def build(boxIdToFund: ErgoId, funderPK: Array[Byte],
            minFee: Long, repaymentTokenId: ErgoId)(implicit ctx: BlockchainContext): SLTFundRepaymentBoxContract = {

    SLTFundRepaymentBoxContract(
      Contract.build(ExleContracts.SLTFundRepaymentBoxProxyContract.contractScript,
        "_BoxIdToFund" -> boxIdToFund,
        "_FunderPK" -> Colls.fromArray(funderPK),
        "_MinFee" -> minFee,
        "_SLTRepaymentTokenId" -> Colls.fromArray(repaymentTokenId.getBytes)
      ),
      boxIdToFund, funderPK,
      minFee, repaymentTokenId)
  }
}


