package SLErgs

import commons.configs.{SLETokensConfig, Token}
import org.ergoplatform.appkit.ErgoId

object LendServiceTokens extends Token {
  val serviceNFT: ErgoId = ErgoId.create(SLETokensConfig.service)
  val lendToken: ErgoId = ErgoId.create(SLETokensConfig.lend)
  val repaymentToken: ErgoId = ErgoId.create(SLETokensConfig.repayment)
}
