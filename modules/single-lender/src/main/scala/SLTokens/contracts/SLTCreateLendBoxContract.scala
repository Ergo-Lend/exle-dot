package SLTokens.contracts

import commons.contracts.ExleContracts
import contracts.Contract
import org.ergoplatform.appkit.{BlockchainContext, ErgoId}
import sigmastate.eval.Colls

case class SLTCreateLendBoxContract(contract: Contract,
                                    borrowerPK: Array[Byte], loanTokenId: ErgoId, minFee: Long,
                                    refundHeightThreshold: Long, goal: Long, deadlineHeight: Long,
                                    interestRate: Long, repaymentHeightLength: Long, serviceNFTId: ErgoId,
                                    lendTokenId: ErgoId)


object SLTCreateLendBoxContract {
  def build(borrowerPK: Array[Byte], loanTokenId: ErgoId, minFee: Long,
            refundHeightThreshold: Long, goal: Long, deadlineHeight: Long,
            interestRate: Long, repaymentHeightLength: Long, serviceNFTId: ErgoId,
            lendTokenId: ErgoId)(implicit ctx: BlockchainContext): SLTCreateLendBoxContract = {

    SLTCreateLendBoxContract(
      Contract.build(ExleContracts.SLTCreateLendBoxProxyContract.contractScript,
        "_BorrowerPk" -> Colls.fromArray(borrowerPK),
        "_LoanTokenId" -> Colls.fromArray(loanTokenId.getBytes),
        "_MinFee" -> minFee,
        "_RefundHeightThreshold" -> refundHeightThreshold,
        "_Goal" -> goal,
        "_DeadlineHeight" -> deadlineHeight,
        "_InterestRate" -> interestRate,
        "_RepaymentHeightLength" -> repaymentHeightLength,
        "_SLTServiceNFTId" -> Colls.fromArray(serviceNFTId.getBytes),
        "_SLTLendTokenId" -> Colls.fromArray(loanTokenId.getBytes)
      ),
      borrowerPK: Array[Byte], loanTokenId: ErgoId, minFee: Long,
      refundHeightThreshold: Long, goal: Long, deadlineHeight: Long,
      interestRate: Long, repaymentHeightLength: Long, serviceNFTId: ErgoId,
      lendTokenId: ErgoId)
  }
}