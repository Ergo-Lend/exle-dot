package commons.configs

import commons.configs.Configs.readKey
import org.ergoplatform.appkit.{ErgoId, ErgoToken}

trait Token {
  val id: String
  val value: Long

  def toErgoToken: ErgoToken =
    new ErgoToken(id, value)
}

/**
  * This trait is for Classes which holds tokens strings
  */
trait LendTokens {
  val serviceNFTId: ErgoId
  val lendTokenId: ErgoId
  val repaymentTokenId: ErgoId
}

object SLETokensConfig {
  lazy val service: String = readKey("lend.token.service")
  lazy val lend: String = readKey("lend.token.lend")
  lazy val repayment: String = readKey("lend.token.repayment")
}

object SLTTokensConfig {
  lazy val service: String = readKey("slt.token.service")
  lazy val lend: String = readKey("slt.token.lend")
  lazy val repayment: String = readKey("slt.token.repayment")
}

object Tokens {
  lazy val sigUSD: String = readKey("tokens.sigusd")
}
