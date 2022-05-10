package features.base

import node.Client
import play.api.Logger
import play.api.libs.circe.Circe
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request}

import javax.inject._

@Singleton
class SpendController @Inject()(client: Client, val controllerComponents: ControllerComponents)
  extends BaseController with Circe {
  private val logger: Logger = Logger(this.getClass)

  def test(): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      println("test")
      Ok("cool spending test").as("application/json")
  }
}
