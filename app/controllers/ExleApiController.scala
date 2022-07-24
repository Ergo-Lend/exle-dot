abstract class ExleApiController (
    val client: Client,
    val controllerComponents: ControllerComponents
) extends BaseController
    with Circe
    with ExceptionThrowable {
        def test(): Action[AnyContent] = Action
    }