package contracts.SingleLender.Ergs

import contracts._
import core.SingleLender.Ergs.boxes.SLEServiceBox
import core.tokens.LendServiceTokens
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ServiceBoxSpec extends AnyWordSpec with Matchers {
  val serviceBox: SLEServiceBox = buildGenesisServiceBox()
  "Service Box" when {
    client.setClient()
    "Genesis" should {
      client.getClient.execute {
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
