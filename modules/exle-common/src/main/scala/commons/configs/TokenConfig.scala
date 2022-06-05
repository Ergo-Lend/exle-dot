package commons.configs

import commons.configs.Configs.readKey

trait Token

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
