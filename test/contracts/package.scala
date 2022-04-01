import ergotools.client.Client
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{Address, ErgoClient, ErgoProver, InputBox, NetworkType, RestApiErgoClient}

package object contracts {
  val ergoClient: ErgoClient = RestApiErgoClient.create("http://188.34.207.91:9053/", NetworkType.MAINNET, "", "")
  val client: Client = new Client(ergoClient)
  val dummyAddress = Address.create("4MQyML64GnzMxZgm")
  val dummyTxId = "ce552663312afc2379a91f803c93e2b10b424f176fbc930055c10def2fd88a5d"
  val dummyToken = "f5cc03963b64d3542b8cea49d5436666a97f6a2d098b7d3b2220e824b5a91819"

  def dummyProver: ErgoProver = {
    ergoClient.execute{
      ctx =>
        val prover = ctx.newProverBuilder()
          .withDLogSecret(BigInt.apply(0).bigInteger)
          .build()

        return prover
    }
  }

  def buildUserBox(value: Long, index: Short = 0): InputBox = {
    ergoClient.execute {
      ctx =>
        val inputBox = ctx.newTxBuilder().outBoxBuilder()
          .value(value)
          .contract(new ErgoTreeContract(dummyAddress.getErgoAddress.script))
          .build()
          .convertToInputWith(dummyTxId, index)

        return inputBox
    }
  }
}
