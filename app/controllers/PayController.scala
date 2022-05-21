package controllers

import io.circe.Json
import io.circe.syntax._
import org.ergoplatform.appkit.{Address, Parameters}
import pay.{ErgoPayResponse, ErgoPayUtils, Severity}
import play.api.libs.circe.Circe
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import java.util.Base64

import javax.inject.Inject

class PayController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController
    with Circe {

  def roundtrip(address: String): Action[AnyContent] = Action {
    val jsonString = getRoundtrip(address).noSpaces

    Ok(jsonString).as("application/json")
  }

  def getRoundtrip(address: String): Json =
    try {
      val isMainNet: Boolean = ErgoPayUtils.isMainNetAddress(address)
      val amountToSend: Long = Parameters.OneErg
      val sender: Address = Address.create(address)
      val recipient: Address = Address.create(address)

      val reducedTxBytes: Array[Byte] =
        ErgoPayUtils
          .getReducedSendTx(amountToSend, sender, recipient, isMainNet)
          .toBytes

      val response: Json = ErgoPayResponse(
        reducedTx = Base64.getUrlEncoder.encodeToString(reducedTxBytes),
        address = address,
        message = "Here is your 1 erg round trip.",
        messageSeverity = Severity.INFORMATION
      ).asJson

      response
    } catch {
      case e: Throwable => {
        val response: Json = ErgoPayResponse(
          messageSeverity = Severity.ERROR,
          message = e.getMessage
        ).asJson

        response
      }
    }

  def sendOneErg(): Action[Json] = Action(circe.json) { implicit request =>
    try {
      val addressString = getRequestBodyAsString(request, "address")
      val address: Address = Address.create(addressString)
      val amount: Long = getRequestBodyAsLong(request, "amount")
      val isMainNet: Boolean = ErgoPayUtils.isMainNetAddress(addressString)

      val reducedTxBytes: Array[Byte] =
        ErgoPayUtils
          .getReducedSendTx(amount, address, address, isMainNet)
          .toBytes

      val response: Json = ErgoPayResponse(
        reducedTx = Base64.getUrlEncoder.encodeToString(reducedTxBytes),
        address = addressString,
        message = "Here is your 1 erg round trip.",
        messageSeverity = Severity.INFORMATION
      ).asJson

      Ok(response).as("application/json")
    } catch {
      case e: Throwable => throw e
    }
  }
}
