package SLErgs.boxes

import SLErgs.contracts.LendProxyContractService
import commons.common.{StackTrace, Time}
import commons.TxState
import commons.configs.Configs
import commons.errors.PaymentAddressException
import db.dbHandlers.{CreateLendReqDAO, FundLendReqDAO, FundRepaymentReqDAO}
import play.api.Logger

import java.time.LocalDateTime
import javax.inject.Inject

class LendProxyAddress @Inject() (
  lendProxyContractService: LendProxyContractService,
  createLendReqDAO: CreateLendReqDAO,
  fundLendReqDAO: FundLendReqDAO,
  fundRepaymentReqDAO: FundRepaymentReqDAO
) {
  private val logger: Logger = Logger(this.getClass)

  def getLendCreateProxyAddress(
    pk: String,
    name: String,
    description: String,
    creationHeight: Long,
    deadlineHeight: Long,
    goal: Long,
    interestRate: Long,
    repaymentHeightLength: Long,
    writeToDb: Boolean = true
  ): String =
    try {
      val paymentAddress =
        lendProxyContractService.getLendCreateProxyContractString(
          pk,
          deadlineHeight,
          goal,
          interestRate,
          repaymentHeightLength
        )

      if (writeToDb) {
        createLendReqDAO.insert(
          name = name,
          description = description,
          goal = goal,
          creationHeight = creationHeight,
          deadlineHeight = deadlineHeight,
          repaymentHeight = repaymentHeightLength,
          interestRate = interestRate,
          state = TxState.Unsuccessful,
          walletAddress = pk,
          paymentAddress = paymentAddress,
          createTxId = null,
          timeStamp = LocalDateTime.now().toString,
          ttl = Configs.creationDelay + Time.currentTime
        )
      }

      paymentAddress
    } catch {
      case e: Throwable =>
        throw new PaymentAddressException(
          "Error in payment address generation",
          StackTrace.getStackTraceStr(e)
        )
    }

  def getFundLendBoxProxyAddress(
    lendBoxId: String,
    lenderPk: String,
    fundAmount: Long,
    writeToDb: Boolean = true
  ): String =
    try {
      val paymentAddress = lendProxyContractService
        .getFundLendBoxProxyContractString(lendBoxId, lenderPk)
      if (writeToDb) {
        fundLendReqDAO.insert(
          lendBoxId = lendBoxId,
          fundingErgAmount = fundAmount,
          state = TxState.Unsuccessful,
          walletAddress = lenderPk,
          paymentAddress = paymentAddress,
          lendTxID = null,
          timeStamp = LocalDateTime.now().toString,
          ttl = Configs.creationDelay + Time.currentTime
        )
      }

      paymentAddress
    } catch {
      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Error in payment address generation")
    }

  def getFundRepaymentBoxProxyAddress(
    repaymentBoxId: String,
    funderPk: String,
    fundAmount: Long,
    writeToDb: Boolean = true
  ): String =
    try {
      val paymentAddress = lendProxyContractService
        .getFundRepaymentBoxProxyContractString(repaymentBoxId, funderPk)
      if (writeToDb) {
        fundRepaymentReqDAO.insert(
          repaymentBoxId = repaymentBoxId,
          fundingErgAmount = fundAmount,
          state = TxState.Unsuccessful,
          walletAddress = funderPk,
          paymentAddress = paymentAddress,
          repaymentTxID = null,
          timeStamp = LocalDateTime.now().toString,
          ttl = Configs.creationDelay + Time.currentTime
        )
      }

      paymentAddress
    } catch {
      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Error in payment address generation")
    }
}
