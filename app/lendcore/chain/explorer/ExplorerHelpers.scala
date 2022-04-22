package lendcore.chain.explorer

import org.ergoplatform.appkit.{ErgoId, Parameters}

object Helpers {
  def ergToNanoErg(erg: Double): Long = (BigDecimal(erg) * Parameters.OneErg).longValue()
  def nanoErgToErg(nanoErg: Long): Double = (BigDecimal(nanoErg) / Parameters.OneErg).doubleValue()

  def toId(hex: String): ErgoId = ErgoId.create(hex)

  def trunc(str: String): String = {
    str.take(6)+"..."+str.takeRight(6)
  }

}