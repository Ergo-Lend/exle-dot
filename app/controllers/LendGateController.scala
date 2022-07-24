package controllers

class LendGateController @Inject() (
    client: Client,
    controllerComponents: ControllerComponents
) extends EXLEController {

    // Controllers
    val sleController: SLEController = SLEController()
    val sltController: SLTController = SLTController()

    // Action Types
    val LEND: String = "lend"
    val REPAYMENT: String = "repayment"

    // Lend Types
    val SLE: String = "sle"
    val SLT: String = "slt"

    // Token Tickers
    val ERG: String = "ERG"
    val SIGUSD: String = "SigUSD"

    def test(): Action[AnyContent] = Action {
        implicit request: Request[AnyContent] =>
            println("lend gate test")
            Ok("cool").as("application/json")
    }

    def create(): Action[Json] = Action {

        implicit request => 
        
            try {

                logger.info("creating a lend box")
                logger.info("filtering lend request body for lendType")

                // token ticker
                val lendType: String = getRequestBodyAsString(request, "lendType")

                lendType match {
                    case SLE =>  sleController.createLendBox()
                    case SLT => sltController.createLendBox()
                    case _ => Ok("could not created lend box, no valid lendType provided").as("application/json")
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
                val LEND: String = "LEND"
                val REPAYMENT: String = "REPAYMENT"
                
                actionType match {
                    
                    case LEND => {
                        
                        logger.info("actionType is LEND, funding a lend box")
                        logger.info("filtering request body for tockenTicker")
                        
                        val lendType: String = getRequestBodyAsString(request, "lendType")

                        lendType match {
                            case SLE => sleController.fundLendBox()
                            case SLT => sltController.fundLendBox()
                            case _ => Ok("could not fund lend box, no valid lendType provided").as("application/json")
                        }
                    
                    }

                    case REPAYMENT => {

                        logger.info("actionType is repayment, funding a repayment box")
                        logger.info("filtering request body for tockenTicker")
                        
                        val lendType: String = getRequestBodyAsString(request, "lendType")

                        lendType match {
                            case SLE => sleController.fundRepaymentBox()
                            case SLT => sltController.fundRepaymentBox()
                            case _ => Ok("could not fund repayment box, no valid lendType provided").as("application/json")
                        }

                    }

                    case _ => Ok("could not fund boxes, no valid actionType provided").as("application/json")
                }

            } catch {
                case e: Throwable => exception(e, logger)
            }

    }

    def fullFund(): Action[Json] = Action {

        implicit request => 

            try {

                logger.info("funding a repayment box fully")
                logger.info("filtering request body for lendType")

                val lendType: String = getRequestBodyAsString(request, "lendType")

                lendType match {
                    case ERG => sleController.fundRepaymentBoxFully()
                    case SLT => sltController.fundRepaymentBoxFully()
                    case _ => Ok("could not fully fund repayment box, no valid lendType provided").as("application/json")
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
                    
                    case LEND => {
                        
                        logger.info("actionType is lend, getting lend boxes")
                        logger.info("filtering request body for lendType")
                            
                        val lendType: String = getRequestBodyAsString(request, "lendType")

                        lendType match {
                            case SLE => sleController.getLendBoxes(offset, limit)
                            case SLT => sltController.getLendBoxes(offset, limit)
                            case _ => Ok("could not get lend boxes, no valid lendType provided").as("application/json")
                        }
                    
                    }

                    case REPAYMENT => {

                        logger.info("actionType is repayment, getting repayment boxes")
                        logger.info("filerting request body for lendType")

                        val lendType: String = getRequestBodyAsString(request, "lendType")

                        lendType match {
                            case SLE => sleController.getRepaymentBoxes(offset, limit)
                            case SLT => sltController.getRepaymentBoxes(offset, limit)
                            case _ => Ok("could not get repayment boxes, no valid lendType provided").as("application/json")
                        }

                    }

                    case _ => Ok("could not get boxes, no valid actionType provided").as("application/json")

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
                    
                    case LEND => {

                        logger.info("getting lend boxes by id")
                        logger.info("filtering request body for lendType")

                        val lendType: String = getRequestBodyAsString(request, "lendType")

                        lendType match {

                            case SLE => sleController.getLendBoxById(id)
                            case SLT => sltController.getLendBoxById(id)
                            case _ => Ok("could not get lend box by id, no valid lendType provided").as("application/json")

                        }

                    }

                    case REPAYMENT => {
                        
                        logger.info("getting repayment boxes by id")
                        logger.info("filtering request body for lendType")

                        val lendType: String = getRequestBodyAsString(request, "lendType")

                        lendType match {

                            case SLE => sleController.getRepaymentBoxById(id)
                            case SLT => sltController.getRepaymentBoxById(id)
                            case _ => Ok("could not get repayment box by id, no valid lendType provided").as("application/json")

                        }

                    }

                    case _ => Ok("could not get boxes by id, no valid actionType provided").as("application/json")

                }

            } catch {
                case e: Throwable => exception(e, logger)
            }

    }

    def mockCreate(): Action[AnyContent] = Action {

        implicit request =>

            try {

                logger.info("creating a mock lend box")
                logger.info("filtering lend request body for lendType")

                val lendType: String = getRequestBodyAsString(request, "lendType")
                
                lendType match {
                    case SLE => sleController.mockCreateLendBox()
                    case SLT => sltController.mockCreateLendBox()
                    case _ => Ok("could not mock create lend box, no valid lendType provided").as("application/json")
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
                    
                    case LEND => {
                        
                        logger.info("actionType is lend, mock funding a lend box")
                        logger.info("filtering request body for lendType")
                        
                        val lendType: String = getRequestBodyAsString(request, "lendType")

                        lendType match {
                            case SLE => sleController.mockFundLendBox()
                            case SLT => sltController.mockFundLendBox()
                            case _ => Ok("could not mock fund lend box, no valid lendType provided").as("application/json")
                        }
                    
                    }

                    case REPAYMENT => {

                        logger.info("actionType is repayment, mock funding a repayment box")
                        logger.info("filtering request body for lendType")
                        
                        val lendType: String = getRequestBodyAsString(request, "lendType")

                        lendType match {
                            case SLE => sleController.mockFundRepaymentBox()
                            case SLT => sltController.mockFundRepaymentBox()
                            case _ => Ok("could not mock fund repayment box, no valid lendType provided").as("application/json")
                        }

                    }

                    case _ => Ok("could not mock fund box, no valid actionType provided").as("application/json")
                }

            } catch {
                case e: Throwable => exception(e, logger)
            }

    }

    def mockFullFund(): Action[Json] = Action {

        implicit request => 

            try {

                logger.info("mock funding a repayment box fully")

                val lendType: String = getRequestBodyAsString(request, "lendType")

                lendType match {
                    case SLE => sleController.mockFundRepaymentBoxFully()
                    case SLT => sltController.mockFundRepaymentBoxFully()
                    case _ => Ok("could not fully fund repayment box, no valid lendType provided").as("application/json")
                }

            } catch {
                case e: Throwable => exception(e, looger)
            }

    }

}