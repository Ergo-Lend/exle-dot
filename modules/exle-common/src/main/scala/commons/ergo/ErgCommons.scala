package commons.ergo

import org.ergoplatform.appkit.Parameters

object ErgCommons {
  val MinBoxFee: Long = Parameters.MinFee
  val MinMinerFee: Long = Parameters.MinFee
  val InfiniteBoxValue: Long = Long.MaxValue

  def nanoErgsToErgs(nanoErgAmount: Long): Double = {
    val ergsValue = nanoErgAmount.toDouble / Parameters.OneErg

    ergsValue
  }
}
