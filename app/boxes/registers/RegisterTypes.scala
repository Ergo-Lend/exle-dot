package boxes.registers

import org.ergoplatform.appkit.{ErgoType, ErgoValue, JavaHelpers}
import special.collection.Coll

import java.nio.charset.StandardCharsets

object RegisterTypes {
  class CollByteRegister extends Register {
    /**
     * Turns a Coll[Byte] to a string
     * @param collByte
     * @return
     */
    def collByteToString(collByte: Coll[Byte]): String = {
      new String(collByte.toArray, StandardCharsets.UTF_8)
    }

    def arrayByteToString(arrayByte: Array[Byte]): String = {
      new String(arrayByte, StandardCharsets.UTF_8)
    }

    def stringToCollByte(str: String): Array[Byte] = {
      str.getBytes("utf-8")
    }

  }

  class LongRegister extends Register {
  }

  class Register {
    def ergoValueOf(elements: Array[Array[Byte]]): ErgoValue[Coll[Coll[Byte]]] = {
      ErgoValue.of(elements.map(item => {
        ErgoValue.of(IndexedSeq(item: _*).toArray)
      }).map(item => item.getValue).toArray, ErgoType.collType(ErgoType.byteType()))
    }

    def ergoValueOf(elements: Array[Long]): ErgoValue[Coll[Long]] = {
      val longColl = JavaHelpers.SigmaDsl.Colls.fromArray(elements)
      ErgoValue.of(longColl, ErgoType.longType())
    }

    def ergoValueOf(elements: Array[Byte]): ErgoValue[Coll[Byte]] = {
      ErgoValue.of(elements, ErgoType.byteType())
    }
  }
}
