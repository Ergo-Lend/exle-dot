package registers

import org.ergoplatform.appkit.{ErgoType, ErgoValue}
import sigmastate.Values.SigmaBoolean
import special.sigma.SigmaProp

import java.math.BigInteger

/**
  * A register value used in the Box wrapper class.
  * CAUTION: Broken for empty arrays until I find a good fix for type matching
  * @param value Value to be held in the register
  * @tparam T type of underlying value in register
  */
case class RegVal[T](value: T) extends Register[T] {
  // TODO: Fix typing for empty arrays. Maybe use shapeless?
  override def ergoType: ErgoType[_] = {
    value match {
      case i: Int         => ErgoType.integerType()
      case l: Long        => ErgoType.longType()
      case b: Byte        => ErgoType.byteType()
      case bI: BigInteger => ErgoType.bigIntType()
      case sp: SigmaProp  => ErgoType.sigmaPropType()
      case sh: Short      => ErgoType.shortType()
      case bl: Boolean    => ErgoType.booleanType()
      case ar: Array[_]   =>
        RegVal(ar.headOption.getOrElse(
          RegisterTypeException("Could not determine ErgoType for empty array"))
        ).ergoType
      case _ =>
        RegisterTypeException("Could not determine ErgoType for given RegVal")
    }
  }

  // TODO: Fix values for empty arrays
  override def toErgoValue: ErgoValue[_] = {
    value match {
      case i: Int           => ErgoValue.of(value.asInstanceOf[Int])
      case l: Long          => ErgoValue.of(value.asInstanceOf[Long])
      case b: Byte          => ErgoValue.of(value.asInstanceOf[Byte])
      case bI: BigInteger   => ErgoValue.of(value.asInstanceOf[BigInteger])
      case sp: SigmaProp    => ErgoValue.of(value.asInstanceOf[SigmaBoolean])
      case sh: Short        => ErgoValue.of(value.asInstanceOf[Short])
      case bl: Boolean      => ErgoValue.of(value.asInstanceOf[Boolean])
      case ar: Array[_]     =>
        RegVal(ar.headOption.getOrElse(
          RegisterTypeException("Could not determine ErgoValue for empty array"))
        ).toErgoValue
      case _ =>
        RegisterTypeException("Could not determine ErgoValue for given RegVal")
    }
  }
}
