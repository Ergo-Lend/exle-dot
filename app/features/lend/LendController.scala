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

@Singleton
class LendController @Inject()(client: Client, lendProxyAddress: LendProxyAddress, val controllerComponents: ControllerComponents)
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
      Ok("cool").as("application/json")
    } catch {
      case e: Throwable => exception(e, logger)
    }
  }}

  def getLendFundById(lendId: String): Action[AnyContent] = Action {
    implicit request:  Request[AnyContent] =>
      try {
        logger.info("Get Lend Funds by id: " + lendId)
//        val result = lendFundUtils.lendingBoxById(lendId)
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
}
