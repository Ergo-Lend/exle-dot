package features.lend

import config.Configs
import ergotools.client.Client
import features.lend.boxes.LendProxyAddress
import features.{getRequestBodyAsLong, getRequestBodyAsString}
import helpers.{ErgoValidator, ExceptionThrowable}
import io.circe.Json
import play.api.Logger
import play.api.mvc._
import play.api.libs.circe.Circe

import javax.inject._
import scala.collection.mutable.ListBuffer

@Singleton
class LendController @Inject()(client: Client, explorer: LendBoxExplorer, lendProxyAddress: LendProxyAddress, val controllerComponents: ControllerComponents)
  extends BaseController with Circe with ExceptionThrowable {
  private val logger: Logger = Logger(this.getClass)

  def test(): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      println("test")
      Ok("cool").as("application/json")
  }

  def getLendFunds(offset: Int, limit: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] => {
    logger.info("Getting Lending funds")
    try {
      val lendBoxes = explorer.getLendBoxes(offset, limit)
      val lendBoxesJson: ListBuffer[Json] = ListBuffer()
      for (lendBox <- lendBoxes) {
        val lendingProjectDetailsRegister = lendBox.lendingProjectDetailsRegister
        val fundingInfoRegister = lendBox.fundingInfoRegister

        lendBoxesJson += Json.fromFields(List(
          ("id", Json.fromString(lendBox.id.toString)),
          ("name", Json.fromString(lendingProjectDetailsRegister.projectName)),
          ("description", Json.fromString(lendingProjectDetailsRegister.description)),
          ("deadline", Json.fromLong(fundingInfoRegister.deadlineHeight)),
          ("fundingGoal", Json.fromLong(fundingInfoRegister.fundingGoal)),
          ("interestPercent", Json.fromLong(fundingInfoRegister.interestRatePercent))
        ))
      }

      val lendBoxJsonList = Json.fromFields(List(
        ("items", Json.fromValues(lendBoxesJson.toList))
      ))
      Ok(lendBoxJsonList.toString()).as("application/json")
    } catch {
      case e: Throwable => exception(e, logger)
    }
  }}

  def getRepaymentFunds(offset: Int, limit: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] => {
    logger.info("Getting Repayment funds")

    try {
      val repaymentBoxes = explorer.getRepaymentBoxes(offset, limit)
      val repaymentDetailsJson: ListBuffer[Json] = ListBuffer()

      for (repaymentBox <- repaymentBoxes) {
        val lendingProjectDetailsRegister = repaymentBox.lendingProjectDetailsRegister
        val fundingInfoRegister = repaymentBox.fundingInfoRegister
        val repaymentDetailsRegister = repaymentBox.repaymentDetailsRegister

        repaymentDetailsJson += Json.fromFields(List(
          ("id", Json.fromString(repaymentBox.id.toString)),
          ("name", Json.fromString(lendingProjectDetailsRegister.projectName)),
          ("description", Json.fromString(lendingProjectDetailsRegister.description)),
          ("deadline", Json.fromLong(fundingInfoRegister.deadlineHeight)),
          ("fundingGoal", Json.fromLong(fundingInfoRegister.fundingGoal)),
          ("interestPercent", Json.fromLong(fundingInfoRegister.interestRatePercent)),
          ("repaymentAmount", Json.fromLong(repaymentDetailsRegister.repaymentAmount)),
          ("repaymentHeightGoal", Json.fromLong(repaymentDetailsRegister.repaymentHeightGoal)),
          ("fundedHeight", Json.fromLong(repaymentDetailsRegister.fundedHeight)),
          ("totalInterestAmount", Json.fromLong(repaymentDetailsRegister.totalInterestAmount))
        ))
      }

      val lendBoxJsonList = Json.fromFields(List(
        ("items", Json.fromValues(repaymentDetailsJson.toList))
      ))
      Ok(lendBoxJsonList.toString()).as("application/json")
    } catch {
      case e: Throwable => exception(e, logger)
    }
  }}

  def getLendFundById(lendId: String): Action[AnyContent] = Action {
    implicit request:  Request[AnyContent] =>
      try {
        logger.info("Get Lend Funds by id: " + lendId)
        val result = explorer.getLendBox(lendId)
        Ok("cool").as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  /**
   * Instantiates a lending fund.
   * Json :
   * {
   *  name
   *  description
   *  goal
   *  deadlineHeight
   *  walletAddress
   * }
   * @return
   */
  def createLendFund(): Action[Json] = Action(circe.json) {
    implicit request =>
      try {
        logger.info("lend fund creation")

        // account details
        val name: String = getRequestBodyAsString(request, "name")
        val description: String = getRequestBodyAsString(request, "description")
        val walletAddress: String = getRequestBodyAsString(request, "walletAddress")

        // accounting
        val goal: Long = getRequestBodyAsLong(request, "goal")
        val deadlineHeight: Long = getRequestBodyAsLong(request, "deadlineHeight")
        val interestRate: Long = getRequestBodyAsLong(request, "interestRate")
        val repaymentHeightLength: Long = getRequestBodyAsLong(request, "repaymentHeightLength")

        // validation
        if (name.length > 250) throw new Throwable("Name size limit is 250 char")
        if (description.length > 1000) throw new Throwable("description size limit is 1000 char")
        ErgoValidator.validateErgValue(goal)
        println(Configs.networkType)
        ErgoValidator.validateAddress(walletAddress)
        println("hey")
        ErgoValidator.validateDeadline(deadlineHeight)

        // we don't need to validate charity percent
        val paymentAddress = lendProxyAddress.getLendCreateProxyAddress(
          pk = walletAddress,
          name = name,
          description = description,
          deadlineHeight = deadlineHeight + client.getHeight,
          goal = goal,
          interestRate = interestRate,
          repaymentHeightLength = repaymentHeightLength
        )
        val amount = Configs.fee * 4
        val delay = Configs.creationDelay

        val result = Json.fromFields(List(
          ("deadline", Json.fromLong(delay)),
          ("address", Json.fromString(paymentAddress)),
          ("fee", Json.fromLong(amount))
        ))
        Ok(result.toString()).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def fundLendBox(): Action[Json] = Action(circe.json) {
    implicit request =>
      try {
        logger.info("lend fund creation")

        // account details
        val name: String = getRequestBodyAsString(request, "name")
        val description: String = getRequestBodyAsString(request, "description")
        val walletAddress: String = getRequestBodyAsString(request, "walletAddress")

        // accounting
        val goal: Long = getRequestBodyAsLong(request, "goal")
        val deadlineHeight: Long = getRequestBodyAsLong(request, "deadlineHeight")
        val interestRate: Long = getRequestBodyAsLong(request, "interestRate")
        val repaymentHeightLength: Long = getRequestBodyAsLong(request, "repaymentHeightLength")

        // validation
        if (name.length > 250) throw new Throwable("Name size limit is 250 char")
        if (description.length > 1000) throw new Throwable("description size limit is 1000 char")
        ErgoValidator.validateErgValue(goal)
        println(Configs.networkType)
        ErgoValidator.validateAddress(walletAddress)
        println("hey")
        ErgoValidator.validateDeadline(deadlineHeight)

        // we don't need to validate charity percent
        val paymentAddress = lendProxyAddress.getLendCreateProxyAddress(
          pk = walletAddress,
          name = name,
          description = description,
          deadlineHeight = deadlineHeight + client.getHeight,
          goal = goal,
          interestRate = interestRate,
          repaymentHeightLength = repaymentHeightLength
        )
        val amount = Configs.fee * 4
        val delay = Configs.creationDelay

        val result = Json.fromFields(List(
          ("deadline", Json.fromLong(delay)),
          ("address", Json.fromString(paymentAddress)),
          ("fee", Json.fromLong(amount))
        ))
        Ok(result.toString()).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def fundRepaymentBox(): Action[Json] = Action(circe.json) {
    implicit request =>
      try {
        logger.info("lend fund creation")

        // account details
        val name: String = getRequestBodyAsString(request, "name")
        val description: String = getRequestBodyAsString(request, "description")
        val walletAddress: String = getRequestBodyAsString(request, "walletAddress")

        // accounting
        val goal: Long = getRequestBodyAsLong(request, "goal")
        val deadlineHeight: Long = getRequestBodyAsLong(request, "deadlineHeight")
        val interestRate: Long = getRequestBodyAsLong(request, "interestRate")
        val repaymentHeightLength: Long = getRequestBodyAsLong(request, "repaymentHeightLength")

        // validation
        if (name.length > 250) throw new Throwable("Name size limit is 250 char")
        if (description.length > 1000) throw new Throwable("description size limit is 1000 char")
        ErgoValidator.validateErgValue(goal)
        println(Configs.networkType)
        ErgoValidator.validateAddress(walletAddress)
        println("hey")
        ErgoValidator.validateDeadline(deadlineHeight)

        // we don't need to validate charity percent
        val paymentAddress = lendProxyAddress.getLendCreateProxyAddress(
          pk = walletAddress,
          name = name,
          description = description,
          deadlineHeight = deadlineHeight + client.getHeight,
          goal = goal,
          interestRate = interestRate,
          repaymentHeightLength = repaymentHeightLength
        )
        val amount = Configs.fee * 4
        val delay = Configs.creationDelay

        val result = Json.fromFields(List(
          ("deadline", Json.fromLong(delay)),
          ("address", Json.fromString(paymentAddress)),
          ("fee", Json.fromLong(amount))
        ))
        Ok(result.toString()).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }
}
