package contracts

import configs.NodeConfig.SystemNodeConfig
import contracts.ExleContracts.DummyErgoScript
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// @todo kelim Failing at github actions. How do we fix?
class ContractsSpec extends AnyWordSpec with Matchers {
  "getContracts" when {
    "getting Exle Contracts" should {
      "get the right contracts" ignore {
        val dummyErgoScript = "{\n" +
          "  // this is a test\n" +
          "}"

        val retrievedContract = DummyErgoScript.contractScript
        assert(
          retrievedContract.equals(dummyErgoScript),
          s"filePath: ${retrievedContract}"
        )
        println(SystemNodeConfig.nodeUrl)
      }

      "find all contracts" ignore {
        for (contract <- ExleContracts.values) {
          assert(contract.contractScript.isInstanceOf[String])
        }
      }
    }
  }
}
