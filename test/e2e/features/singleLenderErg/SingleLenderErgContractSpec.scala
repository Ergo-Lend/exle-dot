package e2e.features.singleLenderErg

import config.Configs
import features.lend.contracts.proxyContracts.createSingleLenderLendBoxProxyScript
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{ConstantsBuilder, ErgoClient, RestApiErgoClient}
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

    "Create Proxy Box" should {

      "Send Correctly" ignore {
        ergoClient.execute(ctx => {
          val lendCreationProxy = createSingleLenderLendBoxProxyScript
          val creationContract = ctx.compileContract(
            ConstantsBuilder.create()
              .build(),
            lendCreationProxy
          )
        })

      }
    }
  }
}
