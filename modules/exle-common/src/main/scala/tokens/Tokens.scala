package tokens

import config.Configs
import org.ergoplatform.appkit.ErgoId

trait Token

object LendServiceTokens extends Token {
  val lendToken: ErgoId = ErgoId.create(Configs.lendServiceTokens.lendToken)
  val repaymentToken: ErgoId = ErgoId.create(Configs.lendServiceTokens.repaymentToken)
  val nft: ErgoId = ErgoId.create(Configs.lendServiceTokens.nft)

  val lendTokenString: String = Configs.lendServiceTokens.lendToken
  val repaymentTokenString: String = Configs.lendServiceTokens.repaymentToken
  val nftString: String = Configs.lendServiceTokens.nft
}