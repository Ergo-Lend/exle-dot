package pay

import io.circe.Json
import io.circe.syntax.EncoderOps
import org.ergoplatform.appkit.{Address}
import scorex.util.encode.Base64

/**
 * Ergo Pay Processor
 *
 * The goal of this class is to take simple arguments like
 *
 * 1. Sender
 * 2. Recipient
 * 3. Amount Ergs to send
 * 4. Amount Tokens to send
 * 5. Message for Success
 * 6. Message for Error
 *
 * and generate the accurate ErgoPayResponse object to the
 * receiver class to be able to send to whoever directly
 * as an object or a json class.
 */
object ErgoPayProcessor {
  def processErg(sender: String,
              recipient: String,
              amountToSend: Long): Json = {
    try {
      val isMainNet: Boolean = ErgoPayUtils.isMainNetAddress(sender)
      val senderAddress: Address = Address.create(sender)
      val recipientAddress: Address = Address.create(recipient)

      val reducedTxBytes: Array[Byte] =
        ErgoPayUtils.getReducedSendTx(amountToSend, senderAddress, recipientAddress, isMainNet).toBytes

      val response: Json = ErgoPayResponse(
        reducedTx = Base64.encode(reducedTxBytes),
        address = recipient,
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
  }
}
