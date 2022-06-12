import org.ergoplatform.appkit.{ErgoType, ErgoValue}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import registers.{RegVal, RegisterTypeException}

import java.math.BigInteger

class RegisterSpec extends AnyWordSpec with Matchers {
  "Registers" should {
    val arrayByteVal: Array[Byte] = "a".getBytes
    val bigIntVal: BigInteger = new BigInteger("123")
    val shortVal: Short = 1.toShort
    val booleanVal: Boolean = true
    val byteVal: Byte = 123

    val intRegVal: RegVal[Int] = RegVal(123)
    val longRegVal: RegVal[Long] = RegVal(123L)
    val arrayByteRegVal: RegVal[Array[Byte]] = RegVal(arrayByteVal)
    val bigIntRegVal: RegVal[BigInteger] = RegVal(bigIntVal)
    val shortRegVal: RegVal[Short] = RegVal(shortVal)
    val booleanRegVal: RegVal[Boolean] = RegVal(booleanVal)
    val byteRegVal: RegVal[Byte] = RegVal(byteVal)
    val emptyRegVal: RegVal[Array[Int]] = RegVal(Array.emptyIntArray)


    "int val convert to int ergo types" in {
      assert(intRegVal.value == 123)
      assert(intRegVal.ergoType == ErgoType.integerType())
      assert(intRegVal.toErgoValue == ErgoValue.of(123))
    }

    "long val convert to long ergo types" in {
      assert(longRegVal.value == 123L)
      assert(longRegVal.ergoType == ErgoType.longType())
      assert(longRegVal.toErgoValue == ErgoValue.of(123L))
    }

    "array byte val convert to array byte ergo types" in {
      assert(arrayByteRegVal.value sameElements arrayByteVal)
      assert(arrayByteRegVal.ergoType == ErgoType.byteType())
      assert(arrayByteRegVal.toErgoValue == ErgoValue.of(arrayByteVal.headOption.get))
    }

    "bigInt val convert to bigInt ergo types" in {
      assert(bigIntRegVal.value == bigIntVal)
      assert(bigIntRegVal.ergoType == ErgoType.bigIntType())
      assert(bigIntRegVal.toErgoValue == ErgoValue.of(bigIntVal))
    }

    "short val convert to short ergo types" in {
      assert(shortRegVal.value == shortVal)
      assert(shortRegVal.ergoType == ErgoType.shortType())
      assert(shortRegVal.toErgoValue == ErgoValue.of(shortVal))
    }

    "boolean val convert to boolean ergo types" in {
      assert(booleanRegVal.value == booleanVal)
      assert(booleanRegVal.ergoType == ErgoType.booleanType())
      assert(booleanRegVal.toErgoValue == ErgoValue.of(booleanVal))
    }

    "byte val convert to byte ergo types" in {
      assert(byteRegVal.value == byteVal)
      assert(byteRegVal.ergoType == ErgoType.byteType())
      assert(byteRegVal.toErgoValue == ErgoValue.of(byteVal))
    }

    "empty array throws exception" in {
      assert(emptyRegVal.value sameElements Array.emptyIntArray)
      intercept[RegisterTypeException] {
        emptyRegVal.toErgoValue
      }
      intercept[RegisterTypeException] {
        emptyRegVal.ergoType
      }
    }
  }
}
