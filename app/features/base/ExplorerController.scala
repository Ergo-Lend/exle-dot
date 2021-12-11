package features.base

import ergotools.client.Client
import ergotools.explorer.Explorer
import features.getRequestBodyAsString
import helpers.ExceptionThrowable
import io.circe.Json
import play.api.Logger
import play.api.libs.circe.Circe
import play.api.mvc._

import javax.inject._

@Singleton
class ExplorerController @Inject()(client: Client, val controllerComponents: ControllerComponents)
  extends BaseController with Explorer with Circe with ExceptionThrowable {
  private val logger: Logger = Logger(this.getClass)

  def test(): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      val message = "Test Explorer"
      println(message)
      Ok(message).as("application/json")
  }

  def getBoxById: Action[Json] = Action(circe.json) {
    implicit request => {
      try {
        val id: String = getRequestBodyAsString(request, "id")
        val box = getUnspentBoxById(id)
        println(box)
        Ok(box).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
    }
  }

  def getTxsInMempoolByAddress: Action[Json] = Action(circe.json) {
    implicit request => {
      try {
        val address: String = getRequestBodyAsString(request, "address")
        val txs = getTxsInMempoolByAddress(address)
        Ok(txs).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
    }
  }

  def getTxConfirmation: Action[Json] = Action(circe.json) {
    implicit request => {
      try {
        val txId: String = getRequestBodyAsString(request, "txId")
        val confirmationNumber = getConfirmationNumber(txId)
        val result = Json.fromFields(List(
          ("confirmationNumber", Json.fromLong(confirmationNumber))
        ))
        Ok(result.toString()).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
    }
  }
}
