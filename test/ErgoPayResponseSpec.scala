import ergotools.ergopay.{ErgoPayResponse, Severity}
import io.circe.Decoder.Result
import io.circe.Json
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ErgoPayResponseSpec extends AnyWordSpec with Matchers {
  "ErgoPayResponse" should {
    "decode correctly" in {
      val incomingJsonString: String =
        """|{
           |  "message" : "Hello",
           |  "messageSeverity" : "NONE",
           |  "address" : "World",
           |  "reducedTx" : "...",
           |  "replyTo" : "mr"
           |}
           |""".stripMargin

      val incomingJson: Json = parse(incomingJsonString).getOrElse(Json.Null)

      val resultResponse: ErgoPayResponse = new ErgoPayResponse(
        message = "Hello",
        messageSeverity = Severity.NONE,
        address = "World",
        reducedTx = "...",
        replyTo = "mr"
      )
      val decodedResult: Result[ErgoPayResponse] = ErgoPayResponse.decoder.decodeJson(incomingJson)
      decodedResult shouldBe Right(resultResponse)
    }

    "encode correctly" in {
      val exampleResponse = new ErgoPayResponse(
        message = "Hello",
        messageSeverity = Severity.NONE,
        address = "World",
        reducedTx = "...",
        replyTo = "mr"
      )

      val resultString: String =
       """|{
          |  "message" : "Hello",
          |  "messageSeverity" : "NONE",
          |  "address" : "World",
          |  "reducedTx" : "...",
          |  "replyTo" : "mr"
          |}
          |""".stripMargin

      val resultJson: Json = parse(resultString).getOrElse(Json.Null)

      val responseJson: Json = ErgoPayResponse.encoder.apply(exampleResponse)
      responseJson shouldBe resultJson
    }
  }
}
