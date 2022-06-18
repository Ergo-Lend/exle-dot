package commons.boxes.registers

import commons.configs.Configs
import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit.JavaHelpers.JByteRType
import org.ergoplatform.appkit.{Address, ErgoType, ErgoValue, JavaHelpers}
import sigmastate.serialization.ErgoTreeSerializer
import special.collection.Coll

import java.nio.charset.StandardCharsets

object RegisterTypes {

  class CollByteRegister extends Register {

    /**
      * Turns a Coll[Byte] to a string
      * @param collByte
      * @return
      */
    def collByteToString(collByte: Coll[Byte]): String =
      new String(collByte.toArray, StandardCharsets.UTF_8)

    def arrayByteToString(arrayByte: Array[Byte]): String =
      new String(arrayByte, StandardCharsets.UTF_8)

    def stringToCollByte(str: String): Array[java.lang.Byte] =
      str.getBytes("utf-8").map(item => item.asInstanceOf[java.lang.Byte])
  }

  object CollByte {

    /**
      * Turns a Coll[Byte] to a string
      * @param collByte
      * @return
      */
    def collByteToString(collByte: Coll[Byte]): String =
      new String(collByte.toArray, StandardCharsets.UTF_8)

    def arrayByteToString(arrayByte: Array[Byte]): String =
      new String(arrayByte, StandardCharsets.UTF_8)

    def stringToCollByte(str: String): Array[Byte] =
      str.getBytes("utf-8")
  }

  class LongRegister extends Register {}

  class NumberRegister(val value: Long) extends LongRegister {

    def toRegister: ErgoValue[java.lang.Long] =
      ergoValueOf(value)
  }

  class StringRegister(val value: String) extends CollByteRegister {
    def this(collByte: Coll[Byte]) = this(
      new String(collByte.toArray, StandardCharsets.UTF_8)
    )

    def toRegister: ErgoValue[Coll[java.lang.Byte]] =
      ergoValueOf(value.getBytes(StandardCharsets.UTF_8).map(_.asInstanceOf[java.lang.Byte]))
  }

  class AddressRegister(val address: String) extends CollByteRegister {

    def toRegister: ErgoValue[Coll[java.lang.Byte]] = {
      if (address.isEmpty) {
        throw new RuntimeException("Address Register: Found an empty address");
      }
      val borrowerPk = Address.create(address).getErgoAddress.script.bytes

      ergoValueOf(borrowerPk.map(_.asInstanceOf[java.lang.Byte]))
    }
  }

  object AddressRegister {

    def getAddress(addressBytes: Array[Byte]): ErgoAddress = {
      val ergoTree =
        ErgoTreeSerializer.DefaultSerializer.deserializeErgoTree(addressBytes)
      Configs.addressEncoder.fromProposition(ergoTree).get
    }
  }

  class Register {

    def ergoValueOf(
      elements: Array[Array[java.lang.Byte]]
    ): ErgoValue[Coll[Coll[java.lang.Byte]]] = {
      val elementsInColl: Array[Coll[java.lang.Byte]] = elements.map(
        innerArray => ergoValueOf(innerArray).getValue
      )

      val elementsInCollEntirely: Coll[Coll[java.lang.Byte]] =
        JavaHelpers.SigmaDsl.Colls.fromArray(elementsInColl)

      ErgoValue.of(
        elementsInCollEntirely,
        ErgoType.collType(ErgoType.byteType())
      )
    }

    def ergoValueOf(elements: Array[java.lang.Long]): ErgoValue[Coll[java.lang.Long]] = {
      ErgoValue.of(elements, ErgoType.longType())
    }

    def ergoValueOf(elements: Array[java.lang.Byte]): ErgoValue[Coll[java.lang.Byte]] = {
      ErgoValue.of(elements, ErgoType.byteType())
    }

    def ergoValueOf(elements: Long): ErgoValue[java.lang.Long] =
      ErgoValue.of(elements.asInstanceOf[java.lang.Long])
  }
}
