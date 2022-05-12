import io.circe.Json
import play.api.mvc.Request

package object features {

  def getRequestBodyAsString(
    request: Request[Json],
    key: String,
    errorMessage: String = ""
  ): String = {
    val processedErrorMessage = processErrorMessage(key, errorMessage)
    request.body.hcursor
      .downField(key)
      .as[String]
      .getOrElse(throw new Throwable(processedErrorMessage))
  }

  def getRequestBodyAsLong(
    request: Request[Json],
    key: String,
    errorMessage: String = ""
  ): Long = {
    val processedErrorMessage = processErrorMessage(key, errorMessage)
    request.body.hcursor
      .downField(key)
      .as[Long]
      .getOrElse(throw new Throwable(processedErrorMessage))
  }

  def getRequestBodyAsDouble(
    request: Request[Json],
    key: String,
    errorMessage: String = ""
  ): Double = {
    val processedErrorMessage = processErrorMessage(key, errorMessage)
    request.body.hcursor
      .downField(key)
      .as[Double]
      .getOrElse(throw new Throwable(processedErrorMessage))
  }

  def processErrorMessage(key: String, errorMessage: String): String =
    if (errorMessage.isEmpty())
      s"$key field must exist"
    else
      errorMessage.toString()
}
