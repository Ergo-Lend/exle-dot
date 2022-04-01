package contracts

import config.Configs
import ergotools.LendServiceTokens
import features.lend.contracts.proxyContracts.createSingleLenderLendBoxProxyScript
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{ConstantsBuilder, ErgoClient, ErgoToken, Parameters, RestApiErgoClient}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.JavaConverters.seqAsJavaListConverter

class ContractExamplesSpec extends AnyWordSpec with Matchers {
  "Contract Examples" can {

    "Prove a true contract" should {
      ergoClient.execute {
        ctx =>
          val trueContract = ctx.compileContract(ConstantsBuilder.create().build(), "{sigmaProp(true)}")
          val falseContract = ctx.compileContract(ConstantsBuilder.create().build(), "{sigmaProp(false)}")

          "succeed" in {
            val txB = ctx.newTxBuilder()
            val ergsInBox = txB.outBoxBuilder()
              .contract(trueContract)
              .value(1e9.toLong)
              .build()
              .convertToInputWith(dummyTxId, 0)

            val ergsOutBox = txB.outBoxBuilder()
              .contract(falseContract)
              .value(1e9.toLong - Parameters.MinFee)
              .build()

            val tx = txB.boxesToSpend(Seq(ergsInBox).asJava)
              .fee(Parameters.MinFee)
              .outputs(ergsOutBox)
              .sendChangeTo(dummyAddress.getErgoAddress)
              .build()

            try {
              val signed = dummyProver.sign(tx)
            } catch {
              case e: Exception => {
                assert(false)
              }
            }

            assert(true)
          }
      }
    }
  }
}

