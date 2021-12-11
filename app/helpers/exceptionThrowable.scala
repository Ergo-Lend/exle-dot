package helpers

import play.api.mvc.Result
import play.api.mvc.Results.BadRequest

import play.api.Logger

trait ExceptionThrowable {
  // Exceptions
  def exception(e: Throwable, logger: Logger): Result = {
    logger.warn(e.getMessage)
    BadRequest(
      s"""
         |{
         |"success": false,
         |"message": "${e.getMessage}"
         |}
         |""".stripMargin).as("application/json")
  }
}
