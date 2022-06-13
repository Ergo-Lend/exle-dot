package node

import commons.node.Client
import org.ergoplatform.appkit.{Address, InputBox}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ClientSpec extends AnyWordSpec with Matchers {
  "Client" should {
    val client = new Client()
    client.setClient()

    // contract: true && false
    val trueAndFalseAddress: Address = Address.create("m3iBKr65o53izn")

    "get unspent boxes" in {
      try {
        val boxes: List[InputBox] = client.getAllUnspentBox(trueAndFalseAddress)
        assert(boxes.head.getId.toString == "ce7df858a94a0bc189befc80d5f627d9bdee042693802831fdef313ab4a821c8")
      }
    }
  }
}
