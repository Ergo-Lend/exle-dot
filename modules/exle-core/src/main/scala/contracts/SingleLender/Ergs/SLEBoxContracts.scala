package contracts.SingleLender.Ergs

import boxes.Contract
import contracts.ExleContracts
import core.tokens.LendServiceTokens
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ErgoContract, Parameters}

object SLELendBoxContract extends Contract {
  override def getContract(ctx: BlockchainContext): ErgoContract = {
    val sleLendBoxGuardScript: String = ExleContracts.SLELendBoxGuardScript.contractScript

    ctx.compileContract(ConstantsBuilder.create()
      .item("_MinFee", Parameters.MinFee)
      .item("_MinBoxAmount", Parameters.MinFee)
      .item("_SLEServiceNFTId", LendServiceTokens.nft.getBytes)
      .item("_SLELendTokenId", LendServiceTokens.lendToken.getBytes)
      .item("_SLERepaymentTokenId", LendServiceTokens.repaymentToken.getBytes)
      .build(), sleLendBoxGuardScript)
  }
}

object SLERepaymentBoxContract extends Contract {
  def getContract(ctx: BlockchainContext): ErgoContract = {
    val sleRepaymentBoxScript: String = ExleContracts.SLERepaymentBoxGuardScript.contractScript

    ctx.compileContract(ConstantsBuilder.create()
      .item("_MinFee", Parameters.MinFee)
      .item("_SLEServiceBoxNFTId", LendServiceTokens.nft.getBytes)
      .item("_SLERepaymentTokenId", LendServiceTokens.repaymentToken.getBytes)
      .build(), sleRepaymentBoxScript)
  }
}
