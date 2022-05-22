package commons.configs

import commons.configs.NodeConfig.networkType
import org.ergoplatform.ErgoAddressEncoder

object Configs extends ConfigHelper {
  lazy val addressEncoder = new ErgoAddressEncoder(networkType.networkPrefix)

  lazy val creationDelay: Long = readKey("creationDelay").toLong

  lazy val creationThreadInterval
    : Int = readKey("threadIntervals.creation").toInt
  lazy val lendThreadInterval: Int = readKey("threadIntervals.lend").toInt
  lazy val repaymentThreadInterval: Int = readKey("threadIntervals.lend").toInt
  lazy val refundThreadInterval: Int = readKey("threadIntervals.refund").toInt
}
