package controllers

import SLErgs.LendBoxExplorer
import SLErgs.boxes.{LendProxyAddress, SLELendBox, SLERepaymentBox}
import commons.common.ErgoValidator
import commons.BoxState
import commons.configs.Configs
import commons.ergo.ErgCommons
import commons.errors.ExceptionThrowable
import commons.node.Client

import pay.ProxyContractErgoPayResponse
import play.api.Logger
import play.api.libs.circe.Circe
import play.api.mvc._

import io.circe.Json
import javax.inject._

class SLEController @Inject() (
  client: Client,
  explorer: LendBoxExplorer, 
  lendProxyAddress: Lend-ProxyAddress,
  controllerComponents: ControllerComponents
) extends LendController {
  private val logger: Logger = Logger(this.getClass)

  // ===== SLE Test Methods ===== //
  def test(): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      println("sle test")
      Ok("cool").as("application/json")
  }

  // ===== SLE Create and Fund Methods ===== //
  
  /**
    * Instantiates a lending fund.
    * Json :
    * {
    *  name
    *  description
    *  goal
    *  deadlineHeight
    *  walletAddress
    * }
    * @return
    */
  def createLendBox(): Action[Json] = Action(circe.json) { implicit request =>
    try {
      logger.info("sle fund creation")

      // account details
      val name: String = getRequestBodyAsString(request, "name")
      val description: String = getRequestBodyAsString(request, "description")
      val walletAddress: String =
        getRequestBodyAsString(request, "walletAddress")

      // accounting
      val goal: Long = getRequestBodyAsLong(request, "goal")
      val deadlineHeight: Long = getRequestBodyAsLong(request, "deadlineHeight")
      val interestRate: Long =
        (getRequestBodyAsDouble(request, "interestRate") * 10).toLong
      val repaymentHeight: Long =
        getRequestBodyAsLong(request, "repaymentHeight")

      // validation
      if (name.length > 250) throw new Throwable("Name size limit is 250 char")
      if (description.length > 1000)
        throw new Throwable("description size limit is 1000 char")
      if (interestRate < 0)
        throw new Throwable("interest rate cannot be negative")

      ErgoValidator.validateErgValue(goal)
      ErgoValidator.validateAddress(walletAddress)
      ErgoValidator.validateDeadline(deadlineHeight)

      // we don't need to validate charity percent
      val paymentAddress = lendProxyAddress.getLendCreateProxyAddress(
        pk = walletAddress,
        name = name,
        description = description,
        creationHeight = client.getHeight,
        deadlineHeight = client.getHeight + deadlineHeight,
        goal = goal,
        interestRate = interestRate,
        repaymentHeightLength = repaymentHeight
      )

      val proxyContractErgoPayResponse: ProxyContractErgoPayResponse =
        ProxyContractErgoPayResponse.getResponse(
          deadline = Configs.creationDelay,
          sender = walletAddress,
          recipient = paymentAddress,
          ergAmount = SLELendBox.getLendBoxInitiationPayment,
          message =
            s"Send ${SLELendBox.getLendBoxInitiationPayment} to Fund Repayment Proxy Contract"
        )

      Ok(proxyContractErgoPayResponse.toJson).as("application/json")
    } catch {
      case e: Throwable =>
        exception(e, logger)
    }
  }

  /**
    * 
    *
    * @return
    */
  def fundLendBox(): Action[Json] = Action(circe.json) { implicit request =>
    try {
      logger.info("lend fund creation")

      // account details
      val lendBoxId: String = getRequestBodyAsString(request, "boxId")
      val walletAddress: String =
        getRequestBodyAsString(request, "walletAddress")

      ErgoValidator.validateAddress(walletAddress)
      val lendBox = explorer.getLendBox(lendBoxId)
      val wrappedLendBox = new SLELendBox(lendBox)
      val amount = wrappedLendBox.getFundingTotalErgs

      val paymentAddress = lendProxyAddress.getFundLendBoxProxyAddress(
        lendBoxId = lendBoxId,
        lenderPk = walletAddress,
        fundAmount = amount
      )

      val proxyContractErgoPayResponse: ProxyContractErgoPayResponse =
        ProxyContractErgoPayResponse.getResponse(
          deadline = Configs.creationDelay,
          sender = walletAddress,
          recipient = paymentAddress,
          ergAmount = amount,
          message = s"Send ${amount} to Fund Lend Proxy Contract"
        )

      Ok(proxyContractErgoPayResponse.toJson).as("application/json")
    } catch {
      case e: Throwable => exception(e, logger)
    }
  }

  def fundRepaymentBox(): Action[Json] = Action(circe.json) {
    implicit request =>
      try {
        logger.info("repayment funding")

        // account details
        val repaymentBoxId: String = getRequestBodyAsString(request, "boxId")
        val walletAddress: String =
          getRequestBodyAsString(request, "walletAddress")

        // accounting
        val fundAmount: Long = getRequestBodyAsLong(request, "fundAmount")

        ErgoValidator.validateAddress(walletAddress)
        val repaymentBox = explorer.getRepaymentBox(repaymentBoxId)
        val wrappedRepaymentBox = new SLERepaymentBox(repaymentBox)
        val amount = wrappedRepaymentBox.getFundAmount(fundAmount)

        val paymentAddress = lendProxyAddress.getFundRepaymentBoxProxyAddress(
          repaymentBoxId = repaymentBoxId,
          funderPk = walletAddress,
          fundAmount = amount
        )

        val proxyContractErgoPayResponse: ProxyContractErgoPayResponse =
          ProxyContractErgoPayResponse.getResponse(
            deadline = Configs.creationDelay,
            sender = walletAddress,
            recipient = paymentAddress,
            ergAmount = amount,
            message = s"Send ${amount} to Fund Repayment Proxy Contract"
          )

        Ok(proxyContractErgoPayResponse.toJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def fundRepaymentBoxFully(): Action[Json] = Action(circe.json) {
    implicit request =>
      try {
        logger.info("repayment funding")

        // account details
        val repaymentBoxId: String = getRequestBodyAsString(request, "boxId")
        val walletAddress: String =
          getRequestBodyAsString(request, "walletAddress")

        ErgoValidator.validateAddress(walletAddress)

        val repaymentBox = explorer.getRepaymentBox(repaymentBoxId)
        val wrappedRepaymentBox = new SLERepaymentBox(repaymentBox)
        val fundAmount = wrappedRepaymentBox.getFullFundAmount

        val paymentAddress = lendProxyAddress.getFundRepaymentBoxProxyAddress(
          repaymentBoxId = repaymentBoxId,
          funderPk = walletAddress,
          fundAmount = fundAmount
        )

        val proxyContractErgoPayResponse: ProxyContractErgoPayResponse =
          ProxyContractErgoPayResponse.getResponse(
            deadline = Configs.creationDelay,
            sender = walletAddress,
            recipient = paymentAddress,
            ergAmount = fundAmount,
            message = s"Send ${fundAmount} to Fund Repayment Proxy Contract"
          )

        Ok(proxyContractErgoPayResponse.toJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  // ===== SLE Get Methods ===== //
  def getLendBoxes(offset: Int, limit: Int): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      logger.info("Getting Lending funds")

      try {
        val wrappedLendBoxes: List[SLELendBox] =
          explorer.getLendBoxes(offset, limit)
        val lendBoxesJson: List[Json] = wrappedLendBoxes.map(lendBoxToJson(_))

        val lendBoxJsonList = Json.fromFields(
          List(
            ("items", Json.fromValues(lendBoxesJson))
          )
        )

        Ok(lendBoxJsonList.toString()).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def getRepaymentBoxes(offset: Int, limit: Int): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      logger.info("Getting Repayment funds")

      try {
        val wrappedRepaymentBoxes: List[SLERepaymentBox] =
          explorer.getRepaymentBoxes(offset, limit)
        val repaymentBoxesJson: List[Json] =
          wrappedRepaymentBoxes.map(repaymentBoxToJson(_))
        val repaymentBoxJsonList = Json.fromFields(
          List(
            ("items", Json.fromValues(repaymentBoxesJson))
          )
        )

        Ok(repaymentBoxJsonList.toString()).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def getLendBoxById(lendId: String): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      try {
        logger.info("Get Lend Funds by id: " + lendId)
        val lendBox = explorer.getLendBox(lendId)
        if (lendBox.getRegisters.size() > 3) {
          val wrappedRepaymentBox = new SLERepaymentBox(lendBox)
          val repaymentBoxJson = repaymentBoxToJson(wrappedRepaymentBox)

          Ok(repaymentBoxJson).as("application/json")
        } else {
          val wrappedLendBox = new SLELendBox(lendBox)
          val lendBoxJson = lendBoxToJson(wrappedLendBox)
          Ok(lendBoxJson).as("application/json")
        }
        // Check to see if its a repayment box
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def getRepaymentBoxById(lendId: String): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      try {
        logger.info("Get Lend Funds by id: " + lendId)
        val repaymentBox = explorer.getRepaymentBox(lendId)
        val wrappedRepaymentBox = new SLERepaymentBox(repaymentBox)
        val repaymentBoxJson = repaymentBoxToJson(wrappedRepaymentBox)

        Ok(repaymentBoxJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }
  
  // ===== SLE ToJson Methods ===== //
  def lendBoxToJson(wrappedLendBox: LendBox): Json = {
    val lendingProjectDetailsRegister =
      wrappedLendBox.lendingProjectDetailsRegister
    val fundingInfoRegister = wrappedLendBox.fundingInfoRegister
    val borrowerRegister = wrappedLendBox.borrowerRegister
    val lenderRegister = wrappedLendBox.singleLenderRegister

    val fundingGoalInErgs =
      ErgCommons.nanoErgsToErgs(fundingInfoRegister.fundingGoal)
    val fullyFunded: Boolean =
      wrappedLendBox.value >= fundingInfoRegister.fundingGoal

    Json.fromFields(
      List(
        ("id", Json.fromString(wrappedLendBox.id.toString)),
        ("name", Json.fromString(lendingProjectDetailsRegister.projectName)),
        (
          "description",
          Json.fromString(lendingProjectDetailsRegister.description)
        ),
        ("value", Json.fromLong(wrappedLendBox.value)),
        ("deadline", Json.fromLong(fundingInfoRegister.deadlineHeight)),
        ("creationHeight", Json.fromLong(fundingInfoRegister.creationHeight)),
        ("fundingGoal", Json.fromLong(fundingInfoRegister.fundingGoal)),
        ("fundingGoalInErgs", Json.fromDoubleOrString(fundingGoalInErgs)),
        ("isFunded", Json.fromBoolean(fullyFunded)),
        (
          "interestRate",
          Json.fromDoubleOrString(fundingInfoRegister.interestRateAsDouble)
        ),
        ("borrowerPk", Json.fromString(borrowerRegister.borrowersAddress)),
        ("lenderPk", Json.fromString(lenderRegister.lendersAddress)),
        ("boxState", Json.fromString(BoxState.Lend.toString))
      )
    )
  }

  def repaymentBoxToJson(wrappedRepaymentBox: SLERepaymentBox): Json = {
    val lendingProjectDetailsRegister =
      wrappedRepaymentBox.lendingProjectDetailsRegister
    val fundingInfoRegister = wrappedRepaymentBox.fundingInfoRegister
    val borrowerRegister = wrappedRepaymentBox.borrowerRegister
    val lenderRegister = wrappedRepaymentBox.singleLenderRegister
    val repaymentDetailsRegister = wrappedRepaymentBox.repaymentDetailsRegister

    val fundingGoalInErgs =
      ErgCommons.nanoErgsToErgs(fundingInfoRegister.fundingGoal)
    val repaymentAmountInErgs =
      ErgCommons.nanoErgsToErgs(repaymentDetailsRegister.repaymentAmount)
    val fullyFunded =
      wrappedRepaymentBox.value >= repaymentDetailsRegister.repaymentAmount

    // test
    Json.fromFields(
      List(
        ("id", Json.fromString(wrappedRepaymentBox.id.toString)),
        ("name", Json.fromString(lendingProjectDetailsRegister.projectName)),
        (
          "description",
          Json.fromString(lendingProjectDetailsRegister.description)
        ),
        ("value", Json.fromLong(wrappedRepaymentBox.value)),
        ("deadline", Json.fromLong(fundingInfoRegister.deadlineHeight)),
        ("creationHeight", Json.fromLong(fundingInfoRegister.creationHeight)),
        (
          "fundingGoalInNanoErgs",
          Json.fromLong(fundingInfoRegister.fundingGoal)
        ),
        ("fundingGoalInErgs", Json.fromDoubleOrString(fundingGoalInErgs)),
        ("borrowerPk", Json.fromString(borrowerRegister.borrowersAddress)),
        ("lenderPk", Json.fromString(lenderRegister.lendersAddress)),
        (
          "interestRate",
          Json.fromDoubleOrString(fundingInfoRegister.interestRateAsDouble)
        ),
        (
          "repaymentAmountInNanoErgs",
          Json.fromLong(repaymentDetailsRegister.repaymentAmount)
        ),
        (
          "repaymentAmountInErgs",
          Json.fromDoubleOrString(repaymentAmountInErgs)
        ),
        ("isFunded", Json.fromBoolean(fullyFunded)),
        (
          "repaymentHeightGoal",
          Json.fromLong(repaymentDetailsRegister.repaymentHeightGoal)
        ),
        ("fundedHeight", Json.fromLong(repaymentDetailsRegister.fundedHeight)),
        ("boxState", Json.fromString(BoxState.Repayment.toString))
      )
    )
  }

  // ===== SLE Mock Create and Fund Methods ===== //
  def mockCreateLendBox(): Action[Json] = Action(circe.json) {
    implicit request =>
      try {
        logger.info("lend fund creation")

        // account details
        val name: String = getRequestBodyAsString(request, "name")
        val description: String = getRequestBodyAsString(request, "description")
        val walletAddress: String =
          getRequestBodyAsString(request, "walletAddress")

        // accounting
        val goal: Long = getRequestBodyAsLong(request, "goal")
        val deadlineHeight: Long =
          getRequestBodyAsLong(request, "deadlineHeight")
        val interestRate: Long =
          (getRequestBodyAsDouble(request, "interestRate") * 10).toLong
        val repaymentHeight: Long =
          getRequestBodyAsLong(request, "repaymentHeight")

        // validation
        if (name.length > 250)
          throw new Throwable("Name size limit is 250 char")
        if (description.length > 1000)
          throw new Throwable("description size limit is 1000 char")

        ErgoValidator.validateErgValue(goal)
        ErgoValidator.validateAddress(walletAddress)
        ErgoValidator.validateDeadline(deadlineHeight)

        // we don't need to validate charity percent
        val paymentAddress = lendProxyAddress.getLendCreateProxyAddress(
          pk = walletAddress,
          name = name,
          description = description,
          creationHeight = client.getHeight,
          deadlineHeight = client.getHeight + deadlineHeight,
          goal = goal,
          interestRate = interestRate,
          repaymentHeightLength = repaymentHeight,
          writeToDb = false
        )

        val proxyContractErgoPayResponse: ProxyContractErgoPayResponse =
          ProxyContractErgoPayResponse.getResponse(
            deadline = Configs.creationDelay,
            sender = walletAddress,
            recipient = paymentAddress,
            ergAmount = SLELendBox.getLendBoxInitiationPayment,
            message =
              s"Send ${SLELendBox.getLendBoxInitiationPayment} to Lend Creation Proxy Contract"
          )

        Ok(proxyContractErgoPayResponse.toJson).as("application/json")
      } catch {
        case e: Throwable =>
          exception(e, logger)
      }
  }

  def mockFundLendBox(): Action[Json] = Action(circe.json) { implicit request =>
    try {
      logger.info("lend fund creation")

      // account details
      val lendBoxId: String = getRequestBodyAsString(request, "boxId")
      val walletAddress: String =
        getRequestBodyAsString(request, "walletAddress")

      ErgoValidator.validateAddress(walletAddress)
      val lendBox = explorer.getLendBox(lendBoxId)
      val wrappedLendBox = new SLELendBox(lendBox)
      val amount = wrappedLendBox.getFundingTotalErgs

      val paymentAddress = lendProxyAddress.getFundLendBoxProxyAddress(
        lendBoxId = lendBoxId,
        lenderPk = walletAddress,
        fundAmount = amount,
        writeToDb = false
      )

      val proxyContractErgoPayResponse: ProxyContractErgoPayResponse =
        ProxyContractErgoPayResponse.getResponse(
          deadline = Configs.creationDelay,
          sender = walletAddress,
          recipient = paymentAddress,
          ergAmount = amount,
          message = s"Send ${amount} to Fund Lend Proxy Contract"
        )

      Ok(proxyContractErgoPayResponse.toJson).as("application/json")
    } catch {
      case e: Throwable => exception(e, logger)
    }
  }

  def mockFundRepaymentBox(): Action[Json] = Action(circe.json) {
    implicit request =>
      try {
        logger.info("repayment funding")

        // account details
        val repaymentBoxId: String = getRequestBodyAsString(request, "boxId")
        val walletAddress: String =
          getRequestBodyAsString(request, "walletAddress")

        // accounting
        val fundAmount: Long = getRequestBodyAsLong(request, "fundAmount")

        ErgoValidator.validateAddress(walletAddress)
        val repaymentBox = explorer.getRepaymentBox(repaymentBoxId)
        val wrappedRepaymentBox = new SLERepaymentBox(repaymentBox)
        val amount = wrappedRepaymentBox.getFundAmount(fundAmount)

        val paymentAddress = lendProxyAddress.getFundRepaymentBoxProxyAddress(
          repaymentBoxId = repaymentBoxId,
          funderPk = walletAddress,
          fundAmount = amount,
          writeToDb = false
        )

        val proxyContractErgoPayResponse: ProxyContractErgoPayResponse =
          ProxyContractErgoPayResponse.getResponse(
            deadline = Configs.creationDelay,
            sender = walletAddress,
            recipient = paymentAddress,
            ergAmount = amount,
            message = s"Send ${amount} to Fund Repayment Proxy Contract"
          )

        Ok(proxyContractErgoPayResponse.toJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def mockFundRepaymentBoxFully(): Action[Json] = Action(circe.json) {
    implicit request =>
      try {
        logger.info("repayment funding")

        // account details
        val repaymentBoxId: String = getRequestBodyAsString(request, "boxId")
        val walletAddress: String =
          getRequestBodyAsString(request, "walletAddress")

        ErgoValidator.validateAddress(walletAddress)

        val repaymentBox = explorer.getRepaymentBox(repaymentBoxId)
        val wrappedRepaymentBox = new SLERepaymentBox(repaymentBox)
        val fundAmount = wrappedRepaymentBox.getFullFundAmount

        val paymentAddress = lendProxyAddress.getFundRepaymentBoxProxyAddress(
          repaymentBoxId = repaymentBoxId,
          funderPk = walletAddress,
          fundAmount = fundAmount,
          writeToDb = false
        )

        val proxyContractErgoPayResponse: ProxyContractErgoPayResponse =
          ProxyContractErgoPayResponse.getResponse(
            deadline = Configs.creationDelay,
            sender = walletAddress,
            recipient = paymentAddress,
            ergAmount = fundAmount,
            message = s"Send ${fundAmount} to Fund Repayment Proxy Contract"
          )

        Ok(proxyContractErgoPayResponse.toJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

}
