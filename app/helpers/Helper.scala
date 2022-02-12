package helpers

import config.Configs
import org.ergoplatform.appkit.{ErgoType, ErgoValue, JavaHelpers}
import sigmastate.Values.ErgoTree
import special.collection.Coll

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
  def longListToErgoValue(elements: Array[Long]): ErgoValue[Coll[Long]] = {
    val longColl = JavaHelpers.SigmaDsl.Colls.fromArray(elements)
    ErgoValue.of(longColl, ErgoType.longType())
  }

  def validateErgValue(value: Long): Unit ={
    if (value < 10000) throw new Throwable("Minimum value is 0.00001 Erg")
  }

  def validateAddress(address: String, name: String = "wallet"): ErgoTree ={
    try {
      Configs.addressEncoder.fromString(address).get.script
    } catch {
      case _: Throwable => throw new Throwable(s"Invalid $name address")
    }
  }

  def validateDeadline(currentHeight: Long, value: Long): Unit = {
    val heightDifference = value - currentHeight

    if (heightDifference < 1) throw new Throwable("Deadline should be positive")
    else if (heightDifference > 262800) throw new Throwable("Maximum deadline is 262800 blocks (about 1 year)")
  }
}
