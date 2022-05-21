package payTest

import configs.{GetNodeConfig, NodeConfig}
import org.ergoplatform.appkit.Address
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pay.ErgoPayUtils

class ErgoPaySpec extends AnyWordSpec with Matchers {
  val dummyAddress: Address = Address.create("4MQyML64GnzMxZgm")
  val isMainNet: Boolean = true
  val nodeConfig: NodeConfig = GetNodeConfig.get(isMainNet = isMainNet)

  "isMainNetAddress" should {
    "return true if isMainNet address" in {
      val isMainNet: Boolean = ErgoPayUtils.isMainNetAddress(dummyAddress.toString)

      assert(isMainNet)
    }

    "return the mainnet node url if it is mainnet" in {
      val nodeUrl: String = nodeConfig.nodeUrl

      val ergoPayDefaultNodeUrl: String = ErgoPayUtils.getDefaultNodeUrl(isMainNet)

      assert(nodeUrl == ergoPayDefaultNodeUrl)
    }
  }
}
