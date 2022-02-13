package ergotools.ergopay

import ergotools.ergopay.Severity.Severity
import io.circe._
import io.circe.parser._

case class ErgoPayResponse(message: String,
                           messageSeverity: Severity,
                           address: String,
                           reducedTx: String,
                           replyTo: String = "")

object ErgoPayResponse {
  import io.circe.Decoder
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

  implicit val severityDecoder: Decoder[Severity.Value] = Decoder.decodeEnumeration(Severity)
  implicit val severityEncoder: Encoder[Severity.Value] = Encoder.encodeEnumeration(Severity)
  implicit val decoder: Decoder[ErgoPayResponse] = deriveDecoder[ErgoPayResponse]
  implicit val encoder: Encoder[ErgoPayResponse] = deriveEncoder[ErgoPayResponse]
}


object Severity extends Enumeration {
  type Severity = Value
  val NONE, INFORMATION, WARNING, ERROR = Value
}
