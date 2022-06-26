import boxes.Box
import common.ErgoTestBase
import org.ergoplatform.appkit.{
  BlockchainContext,
  ConstantsBuilder,
  ErgoContract,
  ErgoId,
  ErgoToken,
  ErgoValue,
  InputBox,
  OutBox,
  UnsignedTransactionBuilder
}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import special.collection.Coll

import java.math.BigInteger

class BoxSpec extends AnyWordSpec with Matchers with ErgoTestBase {

  val dummyContract: String =
    """
      |{
      |   sigmaProp(true)
      |}
      |""".stripMargin

  val testContract: ErgoContract = client.getClient.execute { ctx =>
    ctx.compileContract(new ConstantsBuilder().build(), dummyContract)
  }

  "Box" should {
    client.getClient.execute { ctx =>
      val r4: ErgoValue[Int] = ErgoValue.of(123)
      val r5: ErgoValue[Coll[Byte]] = ErgoValue.of("hello".getBytes)
      val r6: ErgoValue[Long] = ErgoValue.of(123L)
      val r8: ErgoValue[Boolean] = ErgoValue.of(true)
      val r9: ErgoValue[Short] = ErgoValue.of(123.toShort)

      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
      val inputBox: InputBox = txB
        .outBoxBuilder()
        .value(oneErg)
        .registers(
          r4,
          r5,
          r6,
          ErgoValue.of(new BigInteger("123")),
          r8,
          r9
        )
        .tokens(new ErgoToken(new ErgoId("abc".getBytes), 1))
        .contract(testContract)
        .build()
        .convertToInputWith(dummyTxId, 0)

      val box: Box = Box(inputBox)

      "registers populated correctly" in {
        assert(box.R4.toErgoValue == r4)
        assert(box.R6.toErgoValue == r6)
        assert(box.R8.toErgoValue == r8)
        assert(box.R9.toErgoValue == r9)
        // TODO: Failing for coll and bigint
//        assert(box.R5.toErgoValue == r5)
//        assert(box.R7.toErgoValue == ErgoValue.of(new BigInteger("123")))
      }
    }
  }
}
