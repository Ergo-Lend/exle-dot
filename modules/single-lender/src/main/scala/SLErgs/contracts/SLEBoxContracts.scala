package SLErgs.contracts

import SLErgs.LendServiceTokens
import commons.boxes.Contract
import commons.configs.ServiceConfig
import commons.contracts.ExleContracts
import commons.ergo.ContractUtils
import org.ergoplatform.appkit._

object SLELendBoxContract extends Contract {

  override def getContract(ctx: BlockchainContext): ErgoContract = {
    val sleLendBoxGuardScript: String =
      ExleContracts.SLELendBoxGuardScript.contractScript

    ctx.compileContract(
      ConstantsBuilder
        .create()
        .item("_MinFee", Parameters.MinFee)
        .item("_MinBoxAmount", Parameters.MinFee)
        .item("_SLEServiceNFTId", LendServiceTokens.serviceNFT.getBytes)
        .item("_SLELendTokenId", LendServiceTokens.lendToken.getBytes)
        .item("_SLERepaymentTokenId", LendServiceTokens.repaymentToken.getBytes)
        .build(),
      sleLendBoxGuardScript
    )
  }
}

object SLERepaymentBoxContract extends Contract {

  def getContract(ctx: BlockchainContext): ErgoContract = {
    val sleRepaymentBoxScript: String =
      ExleContracts.SLERepaymentBoxGuardScript.contractScript

    ctx.compileContract(
      ConstantsBuilder
        .create()
        .item("_MinFee", Parameters.MinFee)
        .item("_SLEServiceBoxNFTId", LendServiceTokens.serviceNFT.getBytes)
        .item("_SLERepaymentTokenId", LendServiceTokens.repaymentToken.getBytes)
        .build(),
      sleRepaymentBoxScript
    )
  }
}

object SLEServiceBoxContract extends Contract {
  val serviceOwner: Address = ServiceConfig.serviceOwner

  def getContract(ctx: BlockchainContext): ErgoContract = {
    val lendBoxHash =
      ContractUtils.getContractScriptHash(SLELendBoxContract.getContract(ctx))
    val repaymentBoxHash = ContractUtils.getContractScriptHash(
      SLERepaymentBoxContract.getContract(ctx)
    )
    val sleServiceBoxGuardScript =
      ExleContracts.SLEServiceBoxGuardScript.contractScript

    ctx.compileContract(
      ConstantsBuilder
        .create()
        .item("_OwnerPk", serviceOwner.getPublicKey)
        .item("_LendBoxHash", lendBoxHash)
        .item("_RepaymentBoxHash", repaymentBoxHash)
        .item("_MinFee", Parameters.MinFee)
        .build(),
      sleServiceBoxGuardScript
    )
  }
}
