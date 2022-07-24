package controllers

import commons.errors.ExceptionThrowable
import commons.node.Client
import commons.boxes.{LendBox, RepaymentBox}

import play.api.mvc.BaseController
import play.api.libs.circe.Circe
import play.api.mvc._

import io.circe.Json
import SLErgs.LendBoxExplorer
import SLErgs.boxes.LendProxyAddress

abstract class LendController (
    val client: Client,
    val explorer: LendBoxExplorer,          // Create abstract class for SLE/SLT ?
    val lendProxyAddress: LendProxyAddress, // create an abstract class for SLE/SLT ?
    val controllerComponents: ControllerComponents
) extends ExleApiController {

        // ===== Lend Test Methods ===== //
        def test(): Action[AnyContent]

        // ===== Lend Create and Fund Methods ===== //
        def createLendBox(): Action[Json]
        def fundLendBox(): Action[Json]

        def fundRepaymentBox(): Action[Json]
        def fundRepaymentBoxFully(): Action[Json]

        // ===== Lend Get Methods ===== //
        def getLendBoxes(offset: Int, limit: Int): Json
        def getRepaymentBoxes(offset: Int, limit: Int): Json

        def getLendBoxById(lendId: String): Action[AnyContent]
        def getRepaymentBoxById(lendId: String): Action[AnyContent]
        
        // ===== Lend ToJson Methods ===== //
        def lendBoxToJson(wrappedLendBox: LendBox): Json
        def repaymentBoxToJson(wrappedRepaymentBox: RepaymentBox): Json

        // ===== Lend Mock Create and Fund Methods ===== //
        def mockCreateLendBox(): Action[AnyContent]
        def mockFundLendBox(): Action[AnyContent]

        def mockFundRepaymentBox(): Action[AnyContent]
        def mockFundRepaymentBoxFully(): Action[AnyContent]

}

