package contracts.SingleLender

import config.Configs
import contracts.{dummyAddress, dummyProver, dummyTxId, ergoClient}
import ergotools.LendServiceTokens
import features.lend.boxes.LendServiceBox
import features.lend.contracts.proxyContracts.createSingleLenderLendBoxProxyScript
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{ConstantsBuilder, ErgoClient, ErgoToken, Parameters, RestApiErgoClient}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.JavaConverters.seqAsJavaListConverter

class ServiceBoxSpec extends AnyWordSpec with Matchers {
  val serviceBox: LendServiceBox = buildGenesisServiceBox()
  "Service Box" when {
    "Genesis" should {
      ergoClient.execute {
        ctx => {
          val txB = ctx.newTxBuilder()
          val inputServiceBox = serviceBox
            .getOutputServiceBox(ctx, txB)
            .convertToInputWith(dummyTxId, 0)

          "have lend service NFT" in {
            assert(inputServiceBox.getTokens.get(0).getValue == 1)
            assert(inputServiceBox.getTokens.get(0).getId == LendServiceTokens.nft)
          }

          "have right amount of Tokens" in {
            // Lend Tokens
            assert(inputServiceBox.getTokens.get(1).getValue == serviceBox.lendTokenAmount)
            assert(inputServiceBox.getTokens.get(1).getId == LendServiceTokens.lendToken)

            // Repayment Tokens
            assert(inputServiceBox.getTokens.get(2).getValue == serviceBox.repaymentTokenAmount)
            assert(inputServiceBox.getTokens.get(2).getId == LendServiceTokens.repaymentToken)
          }
        }
      }
    }
  }
}
