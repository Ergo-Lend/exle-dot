package SLTokens.contracts

import SLTokens.SLTTokens
import commons.contracts.ExleContracts
import commons.ergo.ErgCommons
import contracts.Contract
import org.ergoplatform.appkit.{BlockchainContext, ErgoId}
import sigmastate.eval.Colls

case class SLTRepaymentBoxContract(
  contract: Contract,
  minFee: Long,
  serviceBoxId: ErgoId,
  repaymentTokenId: ErgoId
) {}

object SLTRepaymentBoxContract {

  def build(minFee: Long, serviceBoxId: ErgoId, repaymentTokenId: ErgoId)(
    implicit ctx: BlockchainContext
  ): SLTRepaymentBoxContract = {
    SLTRepaymentBoxContract(
      Contract.build(
        ExleContracts.SLTRepaymentBoxGuardScript.contractScript,
        "_MinFee" -> minFee,
        "_SLTServiceBoxNFTId" -> Colls.fromArray(serviceBoxId.getBytes),
        "_SLTRepaymentTokenId" -> Colls.fromArray(repaymentTokenId.getBytes)
      ),
      minFee,
      serviceBoxId,
      repaymentTokenId
    )
  }

  def getContract(implicit ctx: BlockchainContext): SLTRepaymentBoxContract =
    this.build(
      minFee = ErgCommons.MinBoxFee,
      serviceBoxId = SLTTokens.serviceNFTId,
      repaymentTokenId = SLTTokens.repaymentTokenId
    )
}
