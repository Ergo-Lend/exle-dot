package contracts

import config.Configs
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ContractsSpec extends AnyWordSpec with Matchers {
  "getContracts" when {
    "getting Exle Contracts" should {
      "get the right contracts" in {
        val dummyErgoScript = "{\n" +
          "  // this is a test\n" +
          "}"

        val retrievedContract = ExleContracts.DummyErgoScript.contractScript
        assert(retrievedContract.equals(dummyErgoScript), s"filePath: ${retrievedContract}")
        println(Configs.nodeUrl)
      }
    }
  }
}
