package contracts

import config.Configs
import contracts.ExleContracts.DummyErgoScript
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ContractsSpec extends AnyWordSpec with Matchers {
  "getContracts" when {
    "getting Exle Contracts" should {
      "get the right contracts" in {
        val dummyErgoScript = "{\n" +
          "  // this is a test\n" +
          "}"

        val retrievedContract = DummyErgoScript.contractScript
        assert(
          retrievedContract.equals(dummyErgoScript),
          s"filePath: ${retrievedContract}"
        )
        println(Configs.nodeUrl)
      }

      "find all contracts" in {
        for (contract <- ExleContracts.values) {
          assert(contract.contractScript.isInstanceOf[String])
        }
      }
    }
  }
}
