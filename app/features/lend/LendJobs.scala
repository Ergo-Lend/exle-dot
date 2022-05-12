package features.lend

import akka.actor.{Actor, ActorLogging}
import features.lend.handlers.{
  FinalizeSingleLenderHandler,
  LendFundingHandler,
  LendInitiationHandler,
  RepaymentFundingHandler
}
import play.api.Logger

object JobsUtil {
  val initiation = "singleLenderInitiation"
  val lendFund = "singleLenderFund"
  val lendFinalize = "singleLenderFinalize"
  val repayment = "singleLenderRepayment"
}

class LendJobs(
  lendInitiationHandler: LendInitiationHandler,
  lendFundingHandler: LendFundingHandler,
  repaymentFundingHandler: RepaymentFundingHandler,
  finalizeSingleLenderHandler: FinalizeSingleLenderHandler
) extends Actor
    with ActorLogging {
  private val logger: Logger = Logger(this.getClass)

  def receive = {
    case JobsUtil.initiation =>
      logger.info(
        "SingleLender Lend Initiation handler handling requests. Woop!"
      )
      lendInitiationHandler.handleReqs()

    case JobsUtil.lendFund =>
      logger.info(
        "SingleLender Lend Initiation handler handling requests. Woop!"
      )
      lendFundingHandler.handleReqs()

    case JobsUtil.repayment =>
      logger.info(
        "SingleLender Lend Initiation handler handling requests. Woop!"
      )
      repaymentFundingHandler.handleReqs()

    case JobsUtil.lendFinalize =>
      logger.info(
        "SingleLender Lend Initiation handler handling requests. Woop!"
      )
      finalizeSingleLenderHandler.handleReqs()
  }
}
