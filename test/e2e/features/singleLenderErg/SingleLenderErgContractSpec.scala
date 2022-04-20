package e2e.features.singleLenderErg

import config.Configs
import lendcore.contracts.SingleLender.Ergs.proxyContracts.proxyContracts.createSingleLenderLendBoxProxyScript
import lendcore.core.SingleLender.Ergs.LendServiceTokens
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{ConstantsBuilder, ErgoClient, ErgoToken, RestApiErgoClient}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SingleLenderErgContractSpec extends AnyWordSpec with Matchers {
  val ergoClient: ErgoClient = RestApiErgoClient.create(
    Configs.nodeUrl,
    Configs.networkType,
    "",
  "")
  val addressEncoder = new ErgoAddressEncoder(Configs.networkType.networkPrefix)

  "SingleLenderErgContract" can {

    "Create Proxy Contract" should {
      val serviceNFTToken = new ErgoToken(LendServiceTokens.nft, 1)
      val lendToken = new ErgoToken(LendServiceTokens.lendToken, 100)
      val repaymentToken = new ErgoToken(LendServiceTokens.repaymentToken, 100)

      val txFee = Configs.fee

      ergoClient.execute(ctx => {
        val lendCreationProxyContractString = createSingleLenderLendBoxProxyScript
        val creationContract = ctx.compileContract(
          ConstantsBuilder.create()
            .build(),
          lendCreationProxyContractString
        )
      })

      "Returns tx fee to ErgoLend" ignore {

      }

      "Create a Lend Box" ignore {

      }
    }

    "Fund LendBox Contract" should {

    }
  }
}
