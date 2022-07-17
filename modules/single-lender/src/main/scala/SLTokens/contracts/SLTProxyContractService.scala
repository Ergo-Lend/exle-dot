package SLTokens.contracts

import SLTokens.SLTTokens
import commons.contracts.{ExleContracts, ProxyContracts}
import commons.node.Client
import org.ergoplatform.appkit.{Address, ConstantsBuilder, ErgoContract, ErgoId}
import tokens.SigUSD

import javax.inject.Inject

class SLTProxyContractService @Inject() (client: Client)
    extends ProxyContracts(client) {

  def getSLTLendCreateProxyContract(
    borrowerPk: String,
    loanToken: Array[Byte],
    deadlineHeight: Long,
    goal: Long,
    interestRate: Long,
    repaymentHeightLength: Long
  ): ErgoContract =
    try {
      val sltServiceNFTId = SLTTokens.serviceNFTId.getBytes
      val sltLendTokenId = SLTTokens.lendTokenId.getBytes
      val borrowerAddress =
        Address.create(borrowerPk).getErgoAddress.script.bytes

      val contractConstants = ConstantsBuilder
        .create()
        .item("_BorrowerPk", borrowerAddress)
        .item("_LoanTokenId", loanToken)
        .item("_MinFee", minFee)
        .item("_RefundHeightThreshold", getRefundHeightThreshold)
        .item("_Goal", goal)
        .item("_DeadlineHeight", deadlineHeight)
        .item("_InterestRate", interestRate)
        .item("_RepaymentHeightLength", repaymentHeightLength)
        .item("_SLTServiceNFTId", sltServiceNFTId)
        .item("_SLTLendTokenId", sltLendTokenId)
        .build()

      val proxyContract = compile(
        contractConstants,
        ExleContracts.SLTCreateLendBoxProxyContract.contractScript
      )

      proxyContract
    } catch {
      case e: Exception => throw e
    }

  // SigUSD Implementation of CreateLendBoxProxyContract
  def getSigUSDCreateLendBoxProxyContract(
                                           borrowerPk: String,
                                           deadlineHeight: Long,
                                           goal: Long,
                                           interestRate: Long,
                                           repaymentHeightLength: Long
                                         ): ErgoContract = {
    getSLTLendCreateProxyContract(
      borrowerPk = borrowerPk,
      loanToken = SigUSD.id.getBytes,
      deadlineHeight = deadlineHeight,
      goal = goal,
      interestRate = interestRate,
      repaymentHeightLength = repaymentHeightLength)
  }

  def getSLTFundLendBoxProxyContract(
    lendBoxId: String,
    lenderAddress: String
  ): ErgoContract =
    try {
      val lenderPk =
        Address.create(lenderAddress).getErgoAddress.script.bytes

      val contractConstants = ConstantsBuilder
        .create()
        .item("_BoxIdToFund", ErgoId.create(lendBoxId).getBytes)
        .item("_LenderPk", lenderPk)
        .item("_MinFee", minFee)
        .item("_SLTLendTokenId", SLTTokens.lendTokenId.getBytes)
        .build()

      val proxyContract = compile(
        contractConstants,
        ExleContracts.SLTFundLendBoxProxyContract.contractScript
      )

      proxyContract
    } catch {
      case e: Exception => throw e
    }

  def getSLTFundRepaymentBoxProxyContract(
    repaymentBoxId: String,
    funderAddress: String
  ): ErgoContract =
    try {
      val funderPk =
        Address.create(funderAddress).getErgoAddress.script.bytes

      val contractConstants = ConstantsBuilder
        .create()
        .item("_BoxIdToFund", ErgoId.create(repaymentBoxId).getBytes)
        .item("_FunderPk", funderPk)
        .item("_MinFee", minFee)
        .item("_SLTRepaymentTokenId", SLTTokens.repaymentTokenId.getBytes)
        .build()

      val proxyContract = compile(
        contractConstants,
        ExleContracts.DummyErgoScript.contractScript
      )

      proxyContract
    } catch {
      case e: Exception => throw e
    }
}
