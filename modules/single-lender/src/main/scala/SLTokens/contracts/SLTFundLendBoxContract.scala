package SLTokens.contracts

import commons.contracts.ExleContracts
import contracts.Contract
import org.ergoplatform.appkit.{BlockchainContext, ErgoId}
import sigmastate.eval.Colls

case class SLTFundLendBoxContract(
  contract: Contract,
  boxIdToFund: ErgoId,
  lenderPK: Array[Byte],
  minFee: Long,
  lendTokenId: ErgoId
)

object SLTFundLendBoxContract {

  def build(
    boxIdToFund: ErgoId,
    lenderPK: Array[Byte],
    minFee: Long,
    lendTokenId: ErgoId
  )(implicit ctx: BlockchainContext): SLTFundLendBoxContract =
    SLTFundLendBoxContract(
      Contract.build(
        ExleContracts.SLTFundLendBoxProxyContract.contractScript,
        "_BoxIdToFund" -> boxIdToFund,
        "_LenderPK" -> Colls.fromArray(lenderPK),
        "_MinFee" -> minFee,
        "_SLTLendTokenId" -> Colls.fromArray(lendTokenId.getBytes)
      ),
      boxIdToFund: ErgoId,
      lenderPK: Array[Byte],
      minFee: Long,
      lendTokenId: ErgoId
    )
}
