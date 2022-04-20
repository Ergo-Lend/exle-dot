package services

import akka.actor.{ActorRef, ActorSystem, Props}
import config.Configs
import features.lend.{JobsUtil, LendJobs}
import features.lend.handlers.{FinalizeSingleLenderHandler, LendFundingHandler, LendInitiationHandler, RepaymentFundingHandler}
import lendcore.components.ergo.Client

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class StartupService @Inject()(client: Client,
                               system: ActorSystem,
                               lendInitiationHandler: LendInitiationHandler,
                               lendFundingHandler: LendFundingHandler,
                               repaymentFundingHandler: RepaymentFundingHandler,
                               finalizeSingleLenderHandler: FinalizeSingleLenderHandler) (implicit ec: ExecutionContext){
  println("App Started")
  client.setClient()

  val jobs: ActorRef = system.actorOf(
    Props(
      new LendJobs(
        lendInitiationHandler,
        lendFundingHandler,
        repaymentFundingHandler,
        finalizeSingleLenderHandler)), "scheduler")

//  system.scheduler.scheduleAtFixedRate(
//    initialDelay = 2.seconds,
//    interval = Configs.lendThreadInterval.seconds,
//    receiver = jobs,
//    message = JobsUtil.initiation
//  )
//
//  system.scheduler.scheduleAtFixedRate(
//    initialDelay = 2.seconds,
//    interval = Configs.lendThreadInterval.seconds,
//    receiver = jobs,
//    message = JobsUtil.lendFund
//  )
//
//  system.scheduler.scheduleAtFixedRate(
//    initialDelay = 2.seconds,
//    interval = Configs.lendThreadInterval.seconds,
//    receiver = jobs,
//    message = JobsUtil.repayment
//  )
//
//  system.scheduler.scheduleAtFixedRate(
//    initialDelay = 2.seconds,
//    interval = Configs.lendThreadInterval.seconds,
//    receiver = jobs,
//    message = JobsUtil.lendFinalize
//  )
}