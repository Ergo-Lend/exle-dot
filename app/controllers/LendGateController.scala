package controllers

class LendGateController @Inject() (
    client: Client,
    controllerComponents: ControllerComponents
) extends EXLEController {

    val sleController: SLEController = SLEController()
    val sltController: SLTController = SLTController()

    def test(): Action[AnyContent] = Action {
        implicit request: Request[AnyContent] =>
            println("lend gate test")
            Ok("cool").as("application/json")
    }

    def create(): Action[Json] = Action {

        implicit request => 
        
            try {

                logger.info("creating a lend box")
                logger.info("filtering lend request body for tokenTicker")

                // token ticker
                val tokenTicker: String = getRequestBodyAsString(request, "tokenTicker")
                
                tokenTicker match {
                    case "ERG" =>  sleController.createLendBox()
                    case _ => sltController.createLendBox()
                }

            } catch {
                case e: Throwable => exeption(e, logger)
            }

    }

    def fund(): Action[Json] = Action {

        implicit request =>

            try {

                logger.info("funding a box")
                logger.info("filtering request body for actionType")
                
                val actionType: String = getRequestBodyAsString(request, "actionType")
                
                actionType match {
                    
                    case "lend" => {
                        
                        logger.info("actionType is lend, funding a lend box")
                        logger.info("filtering request body for tockenTicker")
                        
                        val tokenTicker: String = getRequestBodyAsString(request, "tokenTicker")

                        tokenTicker match {
                            case "ERG" => sleController.fundLendBox()
                            case _ => sltController.fundLendBox()
                        }
                    
                    }

                    case _ => {

                        logger.info("actionType is repayment, funding a repayment box")
                        logger.info("filtering request body for tockenTicker")
                        
                        val tokenTicker: String = getRequestBodyAsString(request, "tokenTicker")

                        tokenTicker match {
                            case "ERG" => sleController.fundRepaymentBox()
                            case _ => sltController.fundRepaymentBox()
                        }

                    }
                }

            } catch {
                case e: Throwable => exception(e, logger)
            }

    }

    def fullFund(): Action[Json] = Action {

        implicit request => 

            try {

                logger.info("funding a repayment box fully")

                val tockenTicker: String = getRequestBodyAsString(request, "tokenTicker")

                tokenTicker match {
                    case "ERG" => sleController.fundRepaymentBoxFully()
                    case _ => sltController.fundRepaymentBoxFully()
                }

            } catch {
                case e: Throwable => exception(e, looger)
            }

    }

    def getBoxes(offset: Int, limit: Int): Action[AnyContent] = Action {

        implicit request =>

            try {

                logger.info("getting boxes")
                logger.info("filtering request body for actionType")

                val actionType: String = getRequestBodyAsString(request, "actionType")

                actionType match {
                    
                    case "lend" => {
                        
                        logger.info("actionType is lend, getting lend boxes")
                        logger.info("filtering request body for tockenTicker")
                            
                        val tokenTicker: String = getRequestBodyAsString(request, "tokenTicker")

                        tokenTicker match {
                            case "ERG" => sleController.getLendBoxes(offset, limit)
                            case _ => sltController.getLendBoxes(offset, limit)
                        }
                    
                    }

                    case _ => {

                        logger.info("actionType is repayment, getting repayment boxes")
                        logger.info("filerting request body for tokenTicker")

                        val tokenTicker: String = getRequestBodyAsString(request, "tokenTicker")

                        tokenTicker match {
                            case "ERG" => sleController.getRepaymentBoxes(offset, limit)
                            case _ => sltController.getLendBoxes(offset, limit)
                        }

                    }

                }

            } catch {
                case e: Throwable => exception(e, logger)
            }
        
    }

    def getBoxById(id: String): Action[AnyContent] = Action {

        implicit request =>

            try {

                logger.info("getting boxes by id")
                logger.info("filtering request body for actionType")

                val actionType: String = getRequestBodyAsString(request, "actionType")

                actionType match {
                    
                    case "lend" => {

                        logger.info("getting lend boxes by id")
                        logger.info("filtering request body for tokenTicker")

                        val tokenTicker: String = getRequestBodyAsString(request, "tokenTicker")

                        tokenTicker match {

                            case "ERG" => sleController.getLendBoxById(id)
                            case _ => sltController.getLendBoxById(id)

                        }

                    }

                    case _ => {
                        
                        logger.info("getting repayment boxes by id")
                        logger.info("filtering request body for tokenTicker")

                        val tokenTicker: String = getRequestBodyAsString(request, "tokenTicker")

                        tokenTicker match {

                            case "ERG" => sleController.getRepaymentBoxById(id)
                            case _ => sltController.getRepaymentBoxById(id)

                        }

                    }

                }

            } catch {
                case e: Throwable => exception(e, logger)
            }

    }

    def mockCreate(): Action[AnyContent] = Action {

        implicit request =>

            try {

                logger.info("creating a mock lend box")
                logger.info("filtering lend request body for tokenTicker")

                // token ticker
                val tokenTicker: String = getRequestBodyAsString(request, "tokenTicker")
                
                tokenTicker match {
                    case "ERG" => sleController.mockCreateLendBox()
                    case _ => sltController.mockCreateLendBox()
                }

            } catch {
                case e: Throwable => exception(e, logger)
            }

    }

    def mockFund(): Action[Json] = Action {

        implicit request =>

            try {

                logger.info("mock funding a box")
                logger.info("filtering request body for actionType")
                
                val actionType: String = getRequestBodyAsString(request, "actionType")
                
                actionType match {
                    
                    case "lend" => {
                        
                        logger.info("actionType is lend, mock funding a lend box")
                        logger.info("filtering request body for tockenTicker")
                        
                        val tokenTicker: String = getRequestBodyAsString(request, "tokenTicker")

                        tokenTicker match {
                            case "ERG" => sleController.mockFundLendBox()
                            case _ => sltController.mockFundLendBox()
                        }
                    
                    }

                    case _ => {

                        logger.info("actionType is repayment, mock funding a repayment box")
                        logger.info("filtering request body for tockenTicker")
                        
                        val tokenTicker: String = getRequestBodyAsString(request, "tokenTicker")

                        tokenTicker match {
                            case "ERG" => sleController.mockFundRepaymentBox()
                            case _ => sltController.mockFundRepaymentBox()
                        }

                    }
                }

            } catch {
                case e: Throwable => exception(e, logger)
            }

    }

    def mockFullFund(): Action[Json] = Action {

        implicit request => 

            try {

                logger.info("mock funding a repayment box fully")

                val tockenTicker: String = getRequestBodyAsString(request, "tokenTicker")

                tokenTicker match {
                    case "ERG" => sleController.mockFundRepaymentBoxFully()
                    case _ => sltController.mockFundRepaymentBoxFully()
                }

            } catch {
                case e: Throwable => exception(e, looger)
            }

    }

}
