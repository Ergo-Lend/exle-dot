package SLErgs

import commons.configs.{LendTokens, SLETokensConfig}
import org.ergoplatform.appkit.ErgoId

object LendServiceTokens extends LendTokens {
  override val serviceNFTId: ErgoId = ErgoId.create(SLETokensConfig.service)
  override val lendTokenId: ErgoId = ErgoId.create(SLETokensConfig.lend)

  override val repaymentTokenId: ErgoId =
    ErgoId.create(SLETokensConfig.repayment)
}
