package SLErgs

import commons.configs.NodeConfig.SystemNodeConfig
import commons.contracts.ExleContracts
import commons.contracts.ExleContracts.DummyErgoScript
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// @todo kelim Failing at github actions. How do we fix?
class ContractsSpec extends AnyWordSpec with Matchers {
  "getContracts" when {
    "getting Exle Contracts" should {
      "get the right contracts" in {
        val dummyErgoScript = "{\n" +
          "  // this is a test\n" +
          "  sigmaProp(true)\n" +
          "}"

        val retrievedContract = DummyErgoScript.contractScript
        assert(
          retrievedContract.equals(dummyErgoScript),
          s"retrieved Contract: $retrievedContract, compared Contract: ${dummyErgoScript}"
        )
        println(SystemNodeConfig.nodeUrl)
      }

      "find all contracts" in {
        ExleContracts.values
          .map(x => assert(x.contractScript.isInstanceOf[String]))
      }
    }
  }
}
