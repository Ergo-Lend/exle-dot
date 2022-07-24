package pay

import commons.ergo.ErgCommons.nanoErgsToErgs
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import org.ergoplatform.appkit.{Address, ReducedTransaction}
import pay.Severity.Severity

import java.util.Base64

case class ErgoPayResponse(
  message: String = "",
  messageSeverity: Severity = Severity.NONE,
  address: String = "",
  reducedTx: String = "",
  replyTo: String = ""
)

object Severity extends Enumeration {
  type Severity = Value
  val INFORMATION, WARNING, ERROR, NONE = Value
}

object ErgoPayResponse {

  implicit val severityEncoder: Encoder[Severity.Value] =
    Encoder.encodeEnumeration(Severity)

  implicit val severityDecoder: Decoder[Severity.Value] =
    Decoder.decodeEnumeration(Severity)

  implicit val encodeFieldType: Encoder[ErgoPayResponse] = {
    Encoder.forProduct5(
      "message",
      "messageSeverity",
      "address",
      "reducedTx",
      "replyTo"
    )(ErgoPayResponse.unapply(_).get)
  }

  implicit val decodeFieldType: Decoder[ErgoPayResponse] = {
    Decoder.forProduct5(
      "message",
      "messageSeverity",
      "address",
      "reducedTx",
      "replyTo"
    )(ErgoPayResponse.apply)
  }

  // @todo kii deprecate this
  def get(
    deadline: Long,
    sender: String,
    recipient: String,
    ergAmount: Long,
    message: String = ""
  ): ErgoPayResponse = {
    val senderAddress = Address.create(sender)
    val recipientAddress = Address.create(recipient)
    val isMainNet: Boolean = ErgoPayUtils.isMainNetAddress(sender)

    val reducedTxBytes: Array[Byte] =
      ErgoPayUtils
        .getReducedSendTx(
          ergAmount,
          sender = senderAddress,
          recipient = recipientAddress,
          isMainNet
        )
        .toBytes

    val ergoPayResponseMessage =
      if (message.nonEmpty) message
      else s"Send ${nanoErgsToErgs(ergAmount)} Ergs to $recipient"

    ErgoPayResponse(
      reducedTx = Base64.getUrlEncoder.encodeToString(reducedTxBytes),
      address = recipient,
      message = ergoPayResponseMessage,
      messageSeverity = Severity.INFORMATION
    )
  }

  /**
   * // ========= Controller use ========== //
   * Retrieve an address and reduced tx to give back
   * an ErgoPayResponse. Since we're moving forward
   * with no manual txs, we're gonna be heavily utilizing
   * this
   *
   * @todo LGD Controller to call this
   *
   * @param recipient address of recipient
   * @param reducedTx the tx that is reduced to bytes
   * @param message message for the response
   * @param messageSeverity the severity of this message
   * @param replyTo honestly I dont know what this is for
   * @return
   */
  def getResponse(
    recipient: Address,
    reducedTx: ReducedTransaction,
    message: String = "",
    messageSeverity: Severity = Severity.NONE,
    replyTo: String = ""
         ): ErgoPayResponse = {
    ErgoPayResponse(
      reducedTx = Base64.getUrlEncoder.encodeToString(reducedTx.toBytes),
      address = recipient.getErgoAddress.toString,
      message = message,
      messageSeverity = messageSeverity,
      replyTo = replyTo
    )
  }
}

// @todo kii deprecate
case class ProxyContractErgoPayResponse(
  deadline: Long,
  recipient: String,
  ergAmount: Long,
  ergoPayResponse: ErgoPayResponse
) {

  def toJson: Json =
    this.asJson
}

// @todo kii deprecate
object ProxyContractErgoPayResponse {

  def getResponse(
    deadline: Long,
    sender: String,
    recipient: String,
    ergAmount: Long,
    message: String = ""
  ): ProxyContractErgoPayResponse = {
    val ergoPayResponse: ErgoPayResponse = ErgoPayResponse.get(
      deadline,
      sender,
      recipient,
      ergAmount,
      message
    )

    ProxyContractErgoPayResponse(
      deadline = deadline,
      recipient = recipient,
      ergAmount = ergAmount,
      ergoPayResponse = ergoPayResponse
    )
  }

  def getErgoPayResponse(
    deadline: Long,
    sender: String,
    recipient: String,
    ergAmount: Long,
    message: String = ""
  ): ErgoPayResponse =
    ErgoPayResponse.get(
      deadline,
      sender,
      recipient,
      ergAmount,
      message
    )
}
