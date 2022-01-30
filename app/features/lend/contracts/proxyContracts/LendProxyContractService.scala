package features.lend.contracts.proxyContracts

import config.Configs
import ergotools.LendServiceTokens
import ergotools.client.Client
import org.ergoplatform.appkit.{Address, BlockchainContext, ConstantsBuilder, ErgoContract, ErgoId, Parameters}
import play.api.libs.json.JsResult.Exception
import special.collection.Coll

import javax.inject.Inject

class LendProxyContractService @Inject()(client: Client) {
  def encodeAddress(contract: ErgoContract): String = {
    Configs.addressEncoder.fromProposition(contract.getErgoTree).get.toString
  }

  def getLendCreateProxyContract(pk: String,
                                 deadlineHeight: Long,
                                 goal: Long,
                                 interestRate: Long,
                                 repaymentHeightLength: Long): String = {
    try {
      client.getClient.execute((ctx: BlockchainContext) => {
        val serviceNftToken = ErgoId.create(LendServiceTokens.nftString).getBytes
        val lendToken = ErgoId.create(LendServiceTokens.lendTokenString).getBytes
        val borrowerPk = Address.create(pk).getErgoAddress.script.bytes

        val createLendingBoxProxy = ctx.compileContract(ConstantsBuilder.create()
          .item("borrowerPk", borrowerPk)
          .item("minFee", Parameters.MinFee)
          .item("refundHeightThreshold", ctx.getHeight + ((Configs.creationDelay / 60 / 2) + 1).toLong)
          .item("goal", goal)
          .item("deadlineHeight", deadlineHeight)
          .item("interestRate", interestRate)
          .item("repaymentHeightLength", repaymentHeightLength)
          .item("serviceNFT", serviceNftToken)
          .item("lendToken", lendToken)
          .build(), createSingleLenderLendBoxProxyScript)

        encodeAddress(createLendingBoxProxy)
      })
    } catch {
      case e: Exception => {
        System.out.println(e)
        return e.toString
      }
    }
  }

  def getFundLendBoxProxyContract(lendId: String,
                                  lenderAddress: String): String = {
    client.getClient.execute((ctx: BlockchainContext) => {
      val fundLendBoxProxy = ctx.compileContract(
        ConstantsBuilder.create()
          .item("boxIdToFund", ErgoId.create(lendId).getBytes)
          .item("lenderPk", Address.create(lenderAddress).getErgoAddress.script.bytes)
          .item("minFee", Parameters.MinFee)
          .item("serviceLendToken", LendServiceTokens.lendToken.getBytes)
          .build(), fundSingleLenderLendBoxProxyScript)

      encodeAddress(fundLendBoxProxy)
    })
  }

  def getFundRepaymentBoxProxyContract(repaymentBoxId: String,
                                      funderPk: String): String = {
    client.getClient.execute((ctx: BlockchainContext) => {
      val repaymentBoxProxy = ctx.compileContract(
        ConstantsBuilder.create()
          .item("boxIdToFund", ErgoId.create(repaymentBoxId).getBytes)
          .item("funderPk", Address.create(funderPk).getErgoAddress.script.bytes)
          .item("minFee", Parameters.MinFee)
          .item("serviceRepaymentToken", LendServiceTokens.repaymentToken.getBytes)
          .build(), repaySingleLenderLoanProxyScript)

      encodeAddress(repaymentBoxProxy)
    })
  }
}
