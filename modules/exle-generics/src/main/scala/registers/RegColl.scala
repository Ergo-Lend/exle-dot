package registers

import org.ergoplatform.appkit.{ErgoType, ErgoValue}
import sigmastate.Values.SigmaBoolean
import sigmastate.eval.Colls
import special.sigma.SigmaProp

import java.math.BigInteger

/**
  * A register collection used in Box Wrapper class
  * @param value Value to be held in the register
  * @param collType ErgoType to be used with RegColl
  * @tparam T type of underlying value in register
  */
class RegColl[T](override val value: Array[T], collType: ErgoType[T])
    extends RegVal[Array[T]](value) {

  override def ergoType: ErgoType[_] =
    collType

  override def toErgoValue: ErgoValue[_] =
    ErgoValue.of(Colls.fromArray(value)(collType.getRType), collType)
}
