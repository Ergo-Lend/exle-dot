package features.lend.contracts.proxyContracts

import config.Configs
import ergotools.LendServiceTokens
import ergotools.client.Client
import org.ergoplatform.appkit.{Address, BlockchainContext, ConstantsBuilder, ErgoContract, ErgoId}

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
    client.getClient.execute((ctx: BlockchainContext) => {
      val nftString = LendServiceTokens
      val serviceNftToken = ErgoId.create(LendServiceTokens.nftString).getBytes
      val boxToken = ErgoId.create(LendServiceTokens.lendTokenString).getBytes
      val createLendingBoxProxy = ctx.compileContract(ConstantsBuilder.create()
        .item("borrowerPk", Address.create(pk).getErgoAddress.script.bytes)
        .item("minFee", Configs.fee)
        .item("refundHeightThreshold", ctx.getHeight + ((Configs.creationDelay / 60 / 2) + 1).toLong)
        .item("goal", goal)
        .item("deadlineHeight", deadlineHeight)
        .item("interestRate", interestRate)
        .item("repaymentHeightLength", repaymentHeightLength)
        .item("lendServiceNFT", serviceNftToken)
        .item("lendServiceToken", boxToken)
        .build(), createSingleLenderLendingBoxProxyScript)

      encodeAddress(createLendingBoxProxy)
    })
  }

  def getFundLendingBoxProxyContract(lendId: String, pk: String, lendAmountPlusFee: Long, deadlineHeight: Long): String = {
    client.getClient.execute((ctx: BlockchainContext) => {
      val fundLendingBoxProxy = ctx.compileContract(
        ConstantsBuilder.create()
          .item("tokenId", ErgoId.create(lendId).getBytes)
          .item("lenderAddress", Address.create(pk).getErgoAddress.script.bytes)
          .item("lendAmountPlusFee", lendAmountPlusFee)
          .item("minFee", Configs.fee)
          .item("deadlineHeight", deadlineHeight)
          .build(), fundSingleLenderLendingBoxProxyScript)

      encodeAddress(fundLendingBoxProxy)
    })
  }

  def getRepaymentLoanProxyContract(repaymentBoxId: String, pk: String, repaymentAmount: Long): String = {
    client.getClient.execute((ctx: BlockchainContext) => {
      val repaymentBoxProxy = ctx.compileContract(
        ConstantsBuilder.create()
          .item("tokenId", ErgoId.create(repaymentBoxId).getBytes)
          .item("lenderAddress", Address.create(pk).getErgoAddress.script.bytes)
          .item("lendAmountPlusFee", repaymentAmount)
          .item("minFee", Configs.fee)
          .build(), repaySingleLenderLoanProxyScript)

      encodeAddress(repaymentBoxProxy)
    })
  }
}
