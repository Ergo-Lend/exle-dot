package features.lend.handlers

import config.Configs
import ergotools.TxState
import ergotools.client.Client
import errors.failedTxException
import features.lend.LendBoxExplorer
import features.lend.dao.{RepaymentReq, RepaymentReqDAO}
import features.lend.txs.singleLender.SingleRepaymentTxFactory
import helpers.{StackTrace, Time}
import org.ergoplatform.appkit.{Address, InputBox}
import play.api.Logger

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global

class RepaymentFundingHandler @Inject()(client: Client, lendBoxExplorer: LendBoxExplorer, repaymentReqDAO: RepaymentReqDAO)
  extends ProxyContractTxHandler(client, lendBoxExplorer, repaymentReqDAO) {
  private val logger: Logger = Logger(this.getClass)

  def handleReqs(): Unit = {
    logger.info("Handling Funding requests...")

    repaymentReqDAO.all.onComplete((requests => {
      var lendBoxMap: Map[String, InputBox] = Map()
      requests.get.map(req => {
        try {
          if (req.ttl <= Time.currentTime || req.state == 2) {
            handleRemoval(req)
          } else {
            handleReq(req, lendBoxMap)
          }
        } catch {
          case e: Throwable => logger.error(StackTrace.getStackTraceStr(e))
        }
      })
    }))
  }

  def handleRemoval(req: RepaymentReq): Unit = {
    val paymentAddress = Address.create(req.paymentAddress)
    val unSpentPaymentBoxes = client.getAllUnspentBox(paymentAddress)
    logger.info("removing request" + req.id)

    if (unSpentPaymentBoxes.nonEmpty) {
      try {
        val unSpentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, Configs.infBoxVal)
        if (unSpentPaymentBoxes.getCoveredAmount >= Configs.fee * 4) {
          logger.info(s"")
        }
      } catch {
        case e: Throwable => logger.info("repayment removal failed")
      }
    }
  }

  def handleReq(req: RepaymentReq, lendBoxMap: Map[String, InputBox]): Map[String, InputBox] = {
    try {
      var outputMap = lendBoxMap
      if (isReady(req)) {
        if (client.getHeight >= req.repaymentDeadline) {
          // @todo what do we do when it passes repayment Deadline
          refundBox(req)
        } else {
          if (!outputMap.contains(req.lendToken)) outputMap += (req.lendToken -> lendBoxExplorer.getLendBox(req.lendToken))
          try {
            outputMap += (req.lendToken -> fundLendTx(req, outputMap(req.lendToken)))
            return outputMap
          } catch {
            case e: Throwable => logger.info(s"funding failed for request ${req.id}")
          }
        }
      }

      lendBoxMap
    } catch {
      case _: Throwable =>
        logger.error(s"Error")
        lendBoxMap
    }
  }

  def fundLendTx(req: RepaymentReq, repaymentInputBox: InputBox): InputBox = {
    client.getClient.execute(ctx => {
      try {
        val paymentBoxList = getPaymentBoxes(req)

        val fundLendTx = SingleRepaymentTxFactory.createLenderFundRepaymentTx(repaymentInputBox, paymentBoxList.getBoxes.get(0))

        val signedTx = fundLendTx.runTx(ctx)

        var fundTxId = ctx.sendTransaction(signedTx)

        if (fundTxId == null) throw failedTxException(s"fund lend tx sending failed for ${req.id}")
        else fundTxId = fundTxId.replaceAll("\"", "")

        repaymentReqDAO.updateLendTxId(req.id, fundTxId)
        repaymentReqDAO.updateStateById(req.id, TxState.Mined)

        signedTx.getOutputsToSpend.get(0)
      } catch {
        case _: Throwable => logger.error("Fund Failed")
          repaymentInputBox
      }
    })
  }

  def refundBox(req: RepaymentReq): Unit = {

  }
}
