package explorer

import org.ergoplatform.appkit.{ErgoId, Parameters}

object Helpers {

  def ergToNanoErg(erg: Double): Long =
    (BigDecimal(erg) * Parameters.OneErg).longValue()

  def nanoErgToErg(nanoErg: Long): Double =
    (BigDecimal(nanoErg) / Parameters.OneErg).doubleValue()

  def toId(hex: String): ErgoId = ErgoId.create(hex)

  def trunc(str: String, limit: Int = 6): String =
    str.take(limit) + "..." + str.takeRight(limit)

}
