package config

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{Address, NetworkType}

object Configs extends ConfigHelper {
  lazy val nodeUrl: String = readKey("node.url").replaceAll("/$", "")
  lazy val networkType: NetworkType = if (readKey("node.networkType").toLowerCase.equals("mainnet"))
      NetworkType.MAINNET
    else
      NetworkType.TESTNET
  lazy val addressEncoder = new ErgoAddressEncoder(networkType.networkPrefix)
  lazy val explorerUrl: String = readKey("explorer.url").replaceAll("/$", "")
  lazy val explorerFront: String = readKey("explorer.front").replaceAll("/$", "")

  lazy val fee: Long = readKey("fee").toLong
  lazy val minBoxErg: Long = readKey("box.min").toLong
  lazy val infBoxVal: Long = readKey("box.infBoxVal").toLong
  lazy val serviceOwner: Address = Address.create(readKey("service.owner"))
  lazy val serviceFeeAddress: Address = Address.create(readKey("service.feeAddress"))

  lazy val creationDelay: Int = readKey("creationDelay").toInt

  lazy val creationThreadInterval: Int = readKey("threadIntervals.creation").toInt
  lazy val lendThreadInterval: Int = readKey("threadIntervals.lend").toInt
  lazy val repaymentThreadInterval: Int = readKey("threadIntervals.lend").toInt
  lazy val refundThreadInterval: Int = readKey("threadIntervals.refund").toInt

  lazy val recaptchaKey: String = readKey("recaptcha.key", default = "not-set")
  lazy val recaptchaPubKey: String = readKey("recaptcha.pubKey", default = "not-set")

  object lendServiceTokens {
    lazy val nft: String = readKey("lend.token.nft")
    lazy val lendToken: String = readKey("lend.token.lendToken")
    lazy val repaymentToken: String = readKey("lend.token.repaymentToken")
    lazy val oracle: String = readKey("lend.token.oracle")
  }
}
