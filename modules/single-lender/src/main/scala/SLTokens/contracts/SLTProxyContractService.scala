package SLTokens.contracts

import SLTokens.SLTTokens
import commons.contracts.{ExleContracts, ProxyContracts}
import commons.node.Client
import org.ergoplatform.appkit.{Address, ConstantsBuilder, ErgoContract, ErgoId}
import tokens.SigUSD

import javax.inject.Inject

class SLTProxyContractService @Inject() (client: Client)
    extends ProxyContracts(client) {

  def getSLTLendCreateProxyContract: ErgoContract =
    try {
      val sltServiceNFTId = SLTTokens.serviceNFTId.getBytes
      val sltLendTokenId = SLTTokens.lendTokenId.getBytes

      val contractConstants = ConstantsBuilder
        .create()
        .item("_MinFee", minFee)
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

  def getSLTFundLendBoxProxyContract: ErgoContract =
    try {
      val contractConstants = ConstantsBuilder
        .create()
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

  def getSLTFundRepaymentBoxProxyContract: ErgoContract =
    try {
      val contractConstants = ConstantsBuilder
        .create()
        .item("_MinFee", minFee)
        .item("_SLTRepaymentTokenId", SLTTokens.repaymentTokenId.getBytes)
        .build()

      val proxyContract = compile(
        contractConstants,
        ExleContracts.SLTFundRepaymentBoxProxyContract.contractScript
      )

      proxyContract
    } catch {
      case e: Exception => throw e
    }
}
