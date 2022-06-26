package SLTokens

import commons.configs.{LendTokens, SLTTokensConfig}
import org.ergoplatform.appkit.ErgoId

object SLTTokens extends LendTokens {
  override val serviceNFTId: ErgoId = ErgoId.create(SLTTokensConfig.service)
  override val lendTokenId: ErgoId = ErgoId.create(SLTTokensConfig.lend)

  override val repaymentTokenId: ErgoId =
    ErgoId.create(SLTTokensConfig.repayment)
}
