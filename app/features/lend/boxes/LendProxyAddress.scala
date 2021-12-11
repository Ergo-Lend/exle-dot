package features.lend.boxes

import config.Configs
import ergotools.TxState
import features.lend.contracts.proxyContracts.LendProxyContractService
import features.lend.dao.CreateLendReqDAO
import helpers.{StackTrace, Time}
import play.api.Logger

import java.time.LocalDateTime
import javax.inject.Inject

class LendProxyAddress @Inject()(lendProxyContractService: LendProxyContractService, createLendReqDAO: CreateLendReqDAO){
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
        pk, deadlineHeight, goal, interestRate, repaymentHeightLength)
      createLendReqDAO.insert(name, description, goal, deadlineHeight, repaymentHeightLength, interestRate, TxState.Unsuccessful, pk, paymentAddress,
        null, LocalDateTime.now().toString, Configs.creationDelay + Time.currentTime)
      paymentAddress
    } catch {
      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Error in payment address generation")
    }
  }
}
