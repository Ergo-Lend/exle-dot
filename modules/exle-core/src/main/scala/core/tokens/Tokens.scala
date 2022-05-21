package core.tokens

import configs.{SLETokensConfig, SLTTokensConfig}
import org.ergoplatform.appkit.ErgoId

trait Token

object LendServiceTokens extends Token {
  val serviceNFT: ErgoId = ErgoId.create(SLETokensConfig.service)
  val lendToken: ErgoId = ErgoId.create(SLETokensConfig.lend)
  val repaymentToken: ErgoId = ErgoId.create(SLETokensConfig.repayment)
}

object SLTTokens extends Token {
  val serviceNFT: ErgoId = ErgoId.create(SLTTokensConfig.service)
  val lendToken: ErgoId = ErgoId.create(SLTTokensConfig.lend)
  val repaymentToken: ErgoId = ErgoId.create(SLTTokensConfig.repayment)
}
