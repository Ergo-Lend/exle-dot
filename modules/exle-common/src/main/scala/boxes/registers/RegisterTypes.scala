package boxes.registers

import configs.Configs
import org.ergoplatform.ErgoAddress
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

    def stringToCollByte(str: String): Array[Byte] =
      str.getBytes("utf-8")
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

    def toRegister: ErgoValue[Long] =
      ergoValueOf(value)
  }

  class StringRegister(val value: String) extends CollByteRegister {
    def this(collByte: Coll[Byte]) = this(
      new String(collByte.toArray, StandardCharsets.UTF_8)
    )

    def toRegister: ErgoValue[Coll[Byte]] =
      ergoValueOf(value.getBytes("utf-8"))
  }

  class AddressRegister(val address: String) extends CollByteRegister {

    def toRegister: ErgoValue[Coll[Byte]] = {
      if (address.isEmpty) {
        throw new RuntimeException("Address Register: Found an empty address");
      }
      val borrowerPk = Address.create(address).getErgoAddress.script.bytes

      ergoValueOf(borrowerPk)
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

    def ergoValueOf(elements: Array[Array[Byte]]): ErgoValue[Coll[Coll[Byte]]] =
      ErgoValue.of(
        elements
          .map(item => ErgoValue.of(IndexedSeq(item: _*).toArray))
          .map(item => item.getValue)
          .toArray,
        ErgoType.collType(ErgoType.byteType())
      )

    def ergoValueOf(elements: Array[Long]): ErgoValue[Coll[Long]] = {
      val longColl = JavaHelpers.SigmaDsl.Colls.fromArray(elements)
      ErgoValue.of(longColl, ErgoType.longType())
    }

    def ergoValueOf(elements: Array[Byte]): ErgoValue[Coll[Byte]] = {
      val byteColl = JavaHelpers.SigmaDsl.Colls.fromArray(elements)
      ErgoValue.of(byteColl, ErgoType.byteType())
    }

    def ergoValueOf(elements: Long): ErgoValue[Long] =
      ErgoValue.of(elements)
  }
}
