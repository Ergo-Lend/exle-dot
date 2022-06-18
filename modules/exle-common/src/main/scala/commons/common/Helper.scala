package commons.common

import commons.configs.Configs
import sigmastate.Values.ErgoTree

import java.io.{PrintWriter, StringWriter}
import java.util.Calendar

object StackTrace {

  def getStackTraceStr(e: Throwable): String = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    e.printStackTrace(pw)
    sw.toString
  }
}

object Time {
  def currentTime: Long = Calendar.getInstance().getTimeInMillis / 1000
}

object ErgoValidator {

  def validateErgValue(value: Long): Unit =
    if (value < 10000) throw new Throwable("Minimum value is 0.00001 Erg")

  def validateAddress(address: String, name: String = "wallet"): ErgoTree =
    try {
      Configs.addressEncoder.fromString(address).get.script
    } catch {
      case _: Throwable => throw new Throwable(s"Invalid $name address")
    }

  def validateDeadline(value: Long): Unit =
    if (value < 1) throw new Throwable("Deadline should be positive")
    else if (value > 262800)
      throw new Throwable("Maximum deadline is 262800 blocks (about 1 year)")
}
