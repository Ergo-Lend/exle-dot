package features.base

import ergotools.client.Client
import ergotools.ergopay.{ErgoPay, ErgoPayResponse}
import ergotools.explorer.Explorer
import helpers.ExceptionThrowable
import io.circe.Json
import play.api.libs.circe.Circe
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.Inject

class PayController @Inject()(client: Client, val controllerComponents: ControllerComponents)
    extends BaseController with Explorer with Circe with ExceptionThrowable {

  def roundTrip(address: String): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      val response: ErgoPayResponse = ErgoPay.roundTrip(address)
      val responseJson: Json = ErgoPayResponse.encoder.apply(response)
      Ok(responseJson).as("application/json")
  }
}
