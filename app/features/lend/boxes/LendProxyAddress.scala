package features.lend.boxes

import config.Configs
import ergotools.TxState
import features.lend.contracts.proxyContracts.LendProxyContractService
import features.lend.dao.{CreateLendReqDAO, FundLendReqDAO, FundRepaymentReqDAO}
import helpers.{StackTrace, Time}
import play.api.Logger

import java.time.LocalDateTime
import javax.inject.Inject

class LendProxyAddress @Inject()(lendProxyContractService: LendProxyContractService,
                                 createLendReqDAO: CreateLendReqDAO,
                                 fundLendReqDAO: FundLendReqDAO,
                                 fundRepaymentReqDAO: FundRepaymentReqDAO){
  private val logger: Logger = Logger(this.getClass)

  def getLendCreateProxyAddress(pk: String,
                                name: String,
                                description: String,
                                deadlineHeight: Long,
                                goal: Long,
                                interestRate: Long,
                                repaymentHeightLength: Long): String = {
    try {
      val paymentAddress = lendProxyContractService.getLendCreateProxyContract(
        pk,
        deadlineHeight,
        goal,
        interestRate,
        repaymentHeightLength)

      createLendReqDAO.insert(
        name = name,
        description = description,
        goal = goal,
        deadlineHeight = deadlineHeight,
        repaymentHeight = repaymentHeightLength,
        interestRatePercent = interestRate,
        state = TxState.Unsuccessful,
        walletAddress = pk,
        paymentAddress = paymentAddress,
        createTxId = null,
        timeStamp = LocalDateTime.now().toString,
        ttl = Configs.creationDelay + Time.currentTime)
      paymentAddress
    } catch {
      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Error in payment address generation")
    }
  }

  def getFundLendBoxProxyAddress(lendBoxId: String, lenderPk: String, fundAmount: Long): String= {
    try {
      val paymentAddress = lendProxyContractService.getFundLendBoxProxyContract(lendBoxId, lenderPk)
      fundLendReqDAO.insert(
        lendBoxId = lendBoxId,
        fundingErgAmount = fundAmount,
        state = TxState.Unsuccessful,
        walletAddress = lenderPk,
        paymentAddress = paymentAddress,
        lendTxID = null,
        timeStamp = LocalDateTime.now().toString,
        ttl = Configs.creationDelay + Time.currentTime)

      paymentAddress
    } catch {
      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Error in payment address generation")
    }
  }

  def getFundRepaymentBoxProxyAddress(repaymentBoxId: String, funderPk: String, fundAmount: Long): String= {
    try {
      val paymentAddress = lendProxyContractService.getFundRepaymentBoxProxyContract(repaymentBoxId, funderPk)
      fundRepaymentReqDAO.insert(
        repaymentBoxId = repaymentBoxId,
        fundingErgAmount = fundAmount,
        state = TxState.Unsuccessful,
        walletAddress = funderPk,
        paymentAddress = paymentAddress,
        repaymentTxID = null,
        timeStamp = LocalDateTime.now().toString,
        ttl = Configs.creationDelay + Time.currentTime)

      paymentAddress
    } catch {
      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Error in payment address generation")
    }
  }
}
