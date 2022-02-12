package ergotools

import org.ergoplatform.appkit.Parameters

object ErgUtils {
  def nanoErgsToErgs(nanoErgAmount: Long): Double = {
    val ergsValue = nanoErgAmount.toDouble/Parameters.OneErg

    ergsValue
  }
}
