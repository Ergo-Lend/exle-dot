package SLErgs.contracts

import SLErgs.LendServiceTokens
import commons.configs.Configs
import commons.contracts.ExleContracts
import commons.node.Client
import org.ergoplatform.appkit._
import play.api.libs.json.JsResult.Exception

import javax.inject.Inject

class LendProxyContractService @Inject() (client: Client) {

  def encodeAddress(contract: ErgoContract): String =
    Configs.addressEncoder.fromProposition(contract.getErgoTree).get.toString

  def getLendCreateProxyContractString(
    pk: String,
    deadlineHeight: Long,
    goal: Long,
    interestRate: Long,
    repaymentHeightLength: Long
  ): String =
    encodeAddress(
      getLendCreateProxyContract(
        pk,
        deadlineHeight,
        goal,
        interestRate,
        repaymentHeightLength
      )
    )

  def getLendCreateProxyContract(
    pk: String,
    deadlineHeight: Long,
    goal: Long,
    interestRate: Long,
    repaymentHeightLength: Long
  ): ErgoContract =
    try {
      client.getClient.execute { (ctx: BlockchainContext) =>
        val serviceNftToken =
          ErgoId.create(LendServiceTokens.serviceNFTId.toString).getBytes
        val lendToken =
          ErgoId.create(LendServiceTokens.lendTokenId.toString).getBytes
        val borrowerPk = Address.create(pk).getErgoAddress.script.bytes

        val createLendBoxScript =
          ExleContracts.SLECreateLendBoxProxyContract.contractScript

        val createLendingBoxProxy = ctx.compileContract(
          ConstantsBuilder
            .create()
            .item("_BorrowerPk", borrowerPk)
            .item("_MinFee", Parameters.MinFee)
            .item(
              "_RefundHeightThreshold",
              ctx.getHeight + ((Configs.creationDelay / 60 / 2) + 1).toLong
            )
            .item("_Goal", goal)
            .item("_DeadlineHeight", deadlineHeight)
            .item("_InterestRate", interestRate)
            .item("_RepaymentHeightLength", repaymentHeightLength)
            .item("_SLEServiceNFTId", serviceNftToken)
            .item("_SLELendTokenId", lendToken)
            .build(),
          createLendBoxScript
        )

        createLendingBoxProxy
      }
    } catch {
      case e: Exception =>
        throw e
    }

  def getFundLendBoxProxyContractString(
    lendId: String,
    lenderAddress: String
  ): String =
    encodeAddress(getFundLendBoxProxyContract(lendId, lenderAddress))

  def getFundLendBoxProxyContract(
    lendId: String,
    lenderAddress: String
  ): ErgoContract =
    try {
      client.getClient.execute { (ctx: BlockchainContext) =>
        val fundLendProxyContractScript =
          ExleContracts.SLEFundLendBoxProxyContract.contractScript
        val fundLendBoxProxy = ctx.compileContract(
          ConstantsBuilder
            .create()
            .item("_BoxIdToFund", ErgoId.create(lendId).getBytes)
            .item(
              "_LenderPk",
              Address.create(lenderAddress).getErgoAddress.script.bytes
            )
            .item("_MinFee", Parameters.MinFee)
            .item("_SLELendTokenId", LendServiceTokens.lendTokenId.getBytes)
            .build(),
          fundLendProxyContractScript
        )

        fundLendBoxProxy
      }
    } catch {
      case e: Exception =>
        throw e
    }

  def getFundRepaymentBoxProxyContractString(
    repaymentBoxId: String,
    funderPk: String
  ): String =
    encodeAddress(getFundRepaymentBoxProxyContract(repaymentBoxId, funderPk))

  def getFundRepaymentBoxProxyContract(
    repaymentBoxId: String,
    funderPk: String
  ): ErgoContract =
    client.getClient.execute { (ctx: BlockchainContext) =>
      val repaymentProxyContractScript =
        ExleContracts.SLEFundRepaymentBoxProxyContract.contractScript

      val repaymentBoxProxy = ctx.compileContract(
        ConstantsBuilder
          .create()
          .item("_BoxIdToFund", ErgoId.create(repaymentBoxId).getBytes)
          .item(
            "_FunderPk",
            Address.create(funderPk).getErgoAddress.script.bytes
          )
          .item("_MinFee", Parameters.MinFee)
          .item(
            "_SLERepaymentTokenId",
            LendServiceTokens.repaymentTokenId.getBytes
          )
          .build(),
        repaymentProxyContractScript
      )

      repaymentBoxProxy
    }
}
