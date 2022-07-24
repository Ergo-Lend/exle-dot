package payTest

import common.ErgoTestBase
import commons.configs.{GetNodeConfig, NodeConfig}
import org.ergoplatform.appkit.{Address, Parameters, ReducedTransaction, RestApiErgoClient, UnsignedTransactionBuilder}
import pay.{ErgoPayResponse, ErgoPayUtils, Severity}
import pay.ErgoPayUtils.getReducedSendTx
import pay.Severity.Severity

import java.util.Base64

class ErgoPaySpec extends ErgoTestBase {
  val isMainNet: Boolean = true
  val nodeConfig: NodeConfig = GetNodeConfig.get(isMainNet = isMainNet)

  "isMainNetAddress" should {
    "return true if isMainNet address" in {
      val isMainNet: Boolean =
        ErgoPayUtils.isMainNetAddress(dummyAddress.toString)

      assert(isMainNet)
    }

    "return the mainnet node url if it is mainnet" in {
      val nodeUrl: String = nodeConfig.nodeUrl

      val ergoPayDefaultNodeUrl: String =
        ErgoPayUtils.getDefaultNodeUrl(isMainNet)

      assert(nodeUrl == ergoPayDefaultNodeUrl)
    }
  }

  "Get ErgoPayResponse" should {
    "return right ergopay response" in {
      val sender: Address = ergolendDevAddress
      val recipient: Address = trueAndFalseAddress
      val reducedTx: ReducedTransaction = getReducedSendTx(
        amountToSend = Parameters.MinFee,
        sender = sender,
        recipient = recipient,
        isMainNet = true
      )
      val message: String = "hi"
      val messageSeverity: Severity = Severity.INFORMATION
      val replyTo: String = "hello"

      val ergoPayResponse: ErgoPayResponse = ErgoPayResponse.getResponse(
        recipient = recipient,
        reducedTx = reducedTx,
        message = message,
        messageSeverity = messageSeverity,
        replyTo = replyTo
      )

      assert(Base64.getUrlDecoder.decode(ergoPayResponse.reducedTx) sameElements reducedTx.toBytes)
      assert(Address.create(ergoPayResponse.address).equals(recipient))
      assert(ergoPayResponse.message equals message)
      assert(ergoPayResponse.messageSeverity equals messageSeverity)
      assert(ergoPayResponse.replyTo equals replyTo)
    }
  }
}
