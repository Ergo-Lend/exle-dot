package e2e.features.singleLenderErg

import configs.{Configs, NodeConfig}
import configs.NodeConfig.SystemNodeConfig
import contracts.ExleContracts
import core.tokens.LendServiceTokens
import ergo.ErgCommons
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{
  ConstantsBuilder,
  ErgoClient,
  ErgoToken,
  RestApiErgoClient
}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SingleLenderErgContractSpec extends AnyWordSpec with Matchers {

  val ergoClient: ErgoClient =
    RestApiErgoClient.create(
      SystemNodeConfig.nodeUrl,
      NodeConfig.networkType,
      "",
      ""
    )

  val addressEncoder = new ErgoAddressEncoder(
    NodeConfig.networkType.networkPrefix
  )

  "SingleLenderErgContract".can {

    "Create Proxy Contract" should {
      val serviceNFTToken = new ErgoToken(LendServiceTokens.serviceNFT, 1)
      val lendToken = new ErgoToken(LendServiceTokens.lendToken, 100)
      val repaymentToken = new ErgoToken(LendServiceTokens.repaymentToken, 100)

      val txFee = ErgCommons.MinMinerFee

      ergoClient.execute { ctx =>
        val lendCreationProxyContractString =
          ExleContracts.SLECreateLendBoxProxyContract.contractScript
        val creationContract = ctx.compileContract(
          ConstantsBuilder
            .create()
            .build(),
          lendCreationProxyContractString
        )
      }

      "Returns tx fee to ErgoLend" ignore {}

      "Create a Lend Box" ignore {}
    }

    "Fund LendBox Contract" should {}
  }
}
