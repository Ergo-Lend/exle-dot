package features.lend.contracts.proxyContracts

import config.Configs
import ergotools.{ContractUtils, LendServiceTokens}
import ergotools.client.Client
import features.lend.boxes.SingleLenderLendBoxContract
import org.ergoplatform.appkit.{Address, BlockchainContext, ConstantsBuilder, ErgoContract, ErgoId, Parameters}
import play.api.libs.json.JsResult.Exception

import javax.inject.Inject

class LendProxyContractService @Inject()(client: Client) {
  def encodeAddress(contract: ErgoContract): String = {
    Configs.addressEncoder.fromProposition(contract.getErgoTree).get.toString
  }

  def getLendCreateProxyContractString(pk: String,
                                       deadlineHeight: Long,
                                       goal: Long,
                                       interestRate: Long,
                                       repaymentHeightLength: Long): String = {
    encodeAddress(getLendCreateProxyContract(pk, deadlineHeight, goal, interestRate, repaymentHeightLength))
  }

  def getLendCreateProxyContract(pk: String,
                                 deadlineHeight: Long,
                                 goal: Long,
                                 interestRate: Long,
                                 repaymentHeightLength: Long): ErgoContract = {
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

        createLendingBoxProxy
      })
    } catch {
      case e: Exception =>
        throw e
    }
  }

  def getFundLendBoxProxyContractString(lendId: String,
                                        lenderAddress: String): String = {
    encodeAddress(getFundLendBoxProxyContract(lendId, lenderAddress))
  }

  def getFundLendBoxProxyContract(lendId: String,
                                  lenderAddress: String): ErgoContract = {
    try {
      client.getClient.execute((ctx: BlockchainContext) => {
        val fundLendBoxProxy = ctx.compileContract(
          ConstantsBuilder.create()
            .item("boxIdToFund", ErgoId.create(lendId).getBytes)
            .item("lenderPk", Address.create(lenderAddress).getErgoAddress.script.bytes)
            .item("minFee", Parameters.MinFee)
            .item("serviceLendToken", LendServiceTokens.lendToken.getBytes)
            .build(), fundSingleLenderLendBoxProxyScript)

        fundLendBoxProxy
      })
    } catch {
      case e: Exception =>
        throw e
    }
  }

  def getFundRepaymentBoxProxyContractString(repaymentBoxId: String,
                                             funderPk: String): String = {
    encodeAddress(getFundRepaymentBoxProxyContract(repaymentBoxId, funderPk))
  }

  def getFundRepaymentBoxProxyContract(repaymentBoxId: String,
                                      funderPk: String): ErgoContract = {
    client.getClient.execute((ctx: BlockchainContext) => {
      val repaymentBoxProxy = ctx.compileContract(
        ConstantsBuilder.create()
          .item("boxIdToFund", ErgoId.create(repaymentBoxId).getBytes)
          .item("funderPk", Address.create(funderPk).getErgoAddress.script.bytes)
          .item("minFee", Parameters.MinFee)
          .item("serviceRepaymentToken", LendServiceTokens.repaymentToken.getBytes)
          .build(), repaySingleLenderLoanProxyScript)

      repaymentBoxProxy
    })
  }
}
