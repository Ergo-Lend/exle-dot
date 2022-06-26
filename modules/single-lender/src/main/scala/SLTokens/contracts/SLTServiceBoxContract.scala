package SLTokens.contracts

import commons.configs.ServiceConfig.serviceOwner
import commons.contracts.ExleContracts
import commons.ergo.{ContractUtils, ErgCommons}
import contracts.Contract
import org.ergoplatform.appkit.{Address, BlockchainContext}
import scorex.crypto.hash.Digest32
import sigmastate.basics.DLogProtocol.ProveDlog

case class SLTServiceBoxContract(
  contract: Contract,
  minFee: Long,
  ownerPK: Address,
  lendBoxHash: Array[Byte],
  repaymentBoxHash: Array[Byte]
) {}

object SLTServiceBoxContract {

  def build(
    minFee: Long,
    ownerPK: Address,
    lendBoxHash: Array[Byte],
    repaymentBoxHash: Array[Byte]
  )(implicit ctx: BlockchainContext): SLTServiceBoxContract =
    SLTServiceBoxContract(
      Contract.build(
        ExleContracts.SLTServiceBoxGuardScript.contractScript,
        "_MinFee" -> minFee,
        "_OwnerPK" -> ownerPK.getPublicKey,
        "_SLTLendBoxHash" -> lendBoxHash,
        "_SLTRepaymentBoxHash" -> repaymentBoxHash
      ),
      minFee,
      ownerPK,
      lendBoxHash,
      repaymentBoxHash
    )

  def getContract(implicit ctx: BlockchainContext): SLTServiceBoxContract = {
    val lendBoxHash: Digest32 =
      ContractUtils.getContractScriptHash(
        SLTLendBoxContract.getContract(ctx).contract.ergoContract
      )

    val repaymentBoxHash: Digest32 =
      ContractUtils.getContractScriptHash(
        SLTRepaymentBoxContract.getContract(ctx).contract.ergoContract
      )

    this.build(
      minFee = ErgCommons.MinMinerFee,
      ownerPK = serviceOwner,
      lendBoxHash = lendBoxHash,
      repaymentBoxHash = repaymentBoxHash
    )
  }
}
