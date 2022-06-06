package SLTokens

import commons.configs.{SLTTokensConfig, Token}
import org.ergoplatform.appkit.ErgoId

object SLTTokens extends Token {
  val serviceNFTId: ErgoId = ErgoId.create(SLTTokensConfig.service)
  val lendTokenId: ErgoId = ErgoId.create(SLTTokensConfig.lend)
  val repaymentTokenId: ErgoId = ErgoId.create(SLTTokensConfig.repayment)
}
