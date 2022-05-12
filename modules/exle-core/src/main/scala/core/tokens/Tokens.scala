package core.tokens

import config.Configs
import org.ergoplatform.appkit.ErgoId

trait Token

object LendServiceTokens extends Token {
  val lendToken: ErgoId = ErgoId.create(Configs.lendServiceTokens.lendToken)

  val repaymentToken: ErgoId =
    ErgoId.create(Configs.lendServiceTokens.repaymentToken)
  val nft: ErgoId = ErgoId.create(Configs.lendServiceTokens.nft)
}

object SLTTokens extends Token {
  val serviceNFT: ErgoId = ErgoId.create(Configs.sltTokens.service)
  val lendToken: ErgoId = ErgoId.create(Configs.sltTokens.lend)
  val repaymentToken: ErgoId = ErgoId.create(Configs.sltTokens.repayment)
}
