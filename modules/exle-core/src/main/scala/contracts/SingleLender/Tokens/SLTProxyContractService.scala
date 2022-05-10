package contracts.SingleLender.Tokens

import contracts.{ExleContracts, ProxyContracts}
import core.tokens.SLTTokens
import node.Client
import org.ergoplatform.appkit.{Address, ConstantsBuilder, ErgoContract, ErgoId}

import javax.inject.Inject

class SLTProxyContractService @Inject()(client:Client) extends ProxyContracts(client) {
  def getSLTLendCreateProxyContract(borrowerPk: String,
                                    loanToken: String,
                                    deadlineHeight: Long,
                                    goal: Long,
                                    interestRate: Long,
                                    repaymentHeightLength: Long): ErgoContract =
  {
    try {
      val sltServiceNFTId = SLTTokens.serviceNFT.getBytes
      val sltLendTokenId = SLTTokens.lendToken.getBytes
      val loanTokenId = ErgoId.create(loanToken).getBytes
      val borrowerAddress = Address.create(borrowerPk).getErgoAddress.script.bytes

      val contractConstants = ConstantsBuilder.create()
        .item("_BorrowerPk", borrowerAddress)
        .item("_LoanTokenId", loanTokenId)
        .item("_MinFee", minFee)
        .item("_RefundHeightThreshold", getRefundHeightThreshold)
        .item("_Goal", goal)
        .item("_DeadlineHeight", deadlineHeight)
        .item("_InterestRate", interestRate)
        .item("_RepaymentHeightLength", repaymentHeightLength)
        .item("_SLTServiceNFTId", sltServiceNFTId)
        .item("_SLTLendTokenId", sltLendTokenId)
        .build()

      val proxyContract = compile(contractConstants, ExleContracts.SLTCreateLendBoxProxyContract.contractScript)

      proxyContract
    } catch {
      case e: Exception => throw e
    }
  }
}
