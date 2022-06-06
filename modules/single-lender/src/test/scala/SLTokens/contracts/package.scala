package SLTokens

import commons.configs.{ServiceConfig, Tokens}
import commons.ergo.ErgCommons
import commons.node.Client
import org.ergoplatform.appkit.{
  Address,
  ErgoContract,
  ErgoProver,
  InputBox,
  NetworkType
}

package object contracts {
  val interestRate: Long = 100L // 10%
  val repaymentHeightLength: Long = 100L
  val deadlineHeightLength: Long = 100L
  val goal: Long = 100L
  val loanName: String = "Test Loan"
  val loanDescription: String = "Test Loan Description"
  val loanToken: String = Tokens.sigUSD
  val client: Client = new Client()
  val networkType: NetworkType = NetworkType.TESTNET
  val dummyAddress: Address = Address.create("4MQyML64GnzMxZgm")
  val hackerAddress: Address = Address.create("m3iBKr65o53izn")

  val dummyTxId: String =
    "ce552663312afc2379a91f803c93e2b10b424f176fbc930055c10def2fd88a5d"
  val service: ServiceConfig.type = ServiceConfig
  val serviceFee: Long = service.serviceFee
  val minFee: Long = ErgCommons.MinMinerFee

  def dummyProver: ErgoProver =
    client.getClient.execute { ctx =>
      val prover = ctx
        .newProverBuilder()
        .withDLogSecret(BigInt.apply(0).bigInteger)
        .build()

      return prover
    }

  /**
    * Create Payment Box
    *
    * creates a dummy payment box using ErgoContract
    * @param contract Proxy Contract
    * @param value Value of the input box
    * @return InputBox generated using dummy tx
    */
  def createPaymentBox(contract: ErgoContract, value: Long): InputBox =
    client.getClient.execute { ctx =>
      val txB = ctx.newTxBuilder()

      txB
        .outBoxBuilder()
        .contract(contract)
        .value(value)
        .build()
        .convertToInputWith(dummyTxId, 0)
    }
}
