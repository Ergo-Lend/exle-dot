import common.ErgoTestBase
import contracts.Contract
import org.ergoplatform.appkit.{
  BlockchainContext,
  ConstantsBuilder,
  ErgoContract
}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ContractSpec extends AnyWordSpec with Matchers with ErgoTestBase {
  val dummyConstant1: (String, Long) = ("_DummyConstants1", 123)
  val dummyConstant2: (String, String) = ("_DummyConstants2", "abc")
  val dummyConstant2Subst: (String, String) = ("_DummyConstants2", "321")

  val dummyContractWithConstants: String =
    """
      |{
      |   sigmaProp(_DummyConstants1 == 123 && _DummyConstants2 == "abc")
      |}
      |""".stripMargin

  val constantsBuilder =
    new ConstantsBuilder()
      .item(dummyConstant1._1, dummyConstant1._2)
      .item(dummyConstant2._1, dummyConstant2._2)
      .build()

  val testContract: ErgoContract = client.getClient.execute { ctx =>
    ctx.compileContract(constantsBuilder, dummyContractWithConstants)
  }

  "Building a Contract" should {
    client.getClient.execute { implicit ctx: BlockchainContext =>
      val constants: List[(String, Any)] = List(dummyConstant1, dummyConstant2)
      val contract: Contract = Contract.build(
        script = dummyContractWithConstants,
        constants = constants: _*
      )

      "have the same values" in {
        assert(contract.ergoTree == testContract.getErgoTree)
        assert(contract.address == testContract.getAddress)
        assert(contract.ergoConstants == testContract.getConstants)
      }

      val substContract: Contract =
        contract.substConstants(dummyConstant2Subst._1, dummyConstant2Subst._2)
      val substTestContract: ErgoContract = testContract.substConstant(
        dummyConstant2Subst._1,
        dummyConstant2Subst._2
      )

      "different from the test contract" in {
        assert(substContract.ergoTree != testContract.getErgoTree)
        assert(substContract.address != testContract.getAddress)
        assert(substContract.ergoConstants != testContract.getConstants)
      }

      "same as new subst test contract" in {
        assert(substContract.ergoTree == substTestContract.getErgoTree)
        assert(substContract.address == substTestContract.getAddress)
        assert(substContract.ergoConstants == substTestContract.getConstants)
      }
    }
  }

  "Getting a Contract from ErgoTree" should {
    client.getClient.execute { implicit ctx: BlockchainContext =>
      val contract: Contract = Contract.fromErgoTree(testContract.getErgoTree)

      "have the same values but constants can't be evaluated" in {
        assert(contract.ergoTree == testContract.getErgoTree)
        assert(contract.address == testContract.getAddress)
        // @todo Cheese, can this constants be retrieved if it's not created
        intercept[RuntimeException] {
          assert(contract.ergoConstants == testContract.getConstants)
        }
      }
    }
  }
}
