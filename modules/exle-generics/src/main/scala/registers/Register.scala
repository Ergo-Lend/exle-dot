package registers

import org.ergoplatform.appkit.{ErgoType, ErgoValue}

/**
  * Base trait for all register classes
  *
  * @tparam T type of underlying value in register
  */
trait Register[_] {

  def ergoType: ErgoType[_]

  def toErgoValue: ErgoValue[_]

}
