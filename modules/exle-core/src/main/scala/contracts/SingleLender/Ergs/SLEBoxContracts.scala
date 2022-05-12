package contracts.SingleLender.Ergs

import boxes.Contract
import config.Configs
import contracts.ExleContracts
import core.tokens.LendServiceTokens
import ergo.ContractUtils
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ConstantsBuilder,
  ErgoContract,
  Parameters
}

object SLELendBoxContract extends Contract {

  override def getContract(ctx: BlockchainContext): ErgoContract = {
    val sleLendBoxGuardScript: String =
      ExleContracts.SLELendBoxGuardScript.contractScript

    ctx.compileContract(
      ConstantsBuilder
        .create()
        .item("_MinFee", Parameters.MinFee)
        .item("_MinBoxAmount", Parameters.MinFee)
        .item("_SLEServiceNFTId", LendServiceTokens.nft.getBytes)
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
        .item("_SLEServiceBoxNFTId", LendServiceTokens.nft.getBytes)
        .item("_SLERepaymentTokenId", LendServiceTokens.repaymentToken.getBytes)
        .build(),
      sleRepaymentBoxScript
    )
  }
}

object SLEServiceBoxContract extends Contract {
  val serviceOwner: Address = Configs.serviceOwner

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
