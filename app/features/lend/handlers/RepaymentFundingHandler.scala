package features.lend.handlers

import config.Configs
import ergotools.TxState
import ergotools.client.Client
import errors.failedTxException
import features.lend.LendBoxExplorer
import features.lend.dao.{FundRepaymentReqDAO, FundRepaymentReq}
import features.lend.txs.singleLender.{SingleRepaymentTxFactory}
import helpers.{StackTrace, Time}
import org.ergoplatform.appkit.{Address, InputBox}
import play.api.Logger

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global

class RepaymentFundingHandler @Inject()(client: Client, lendBoxExplorer: LendBoxExplorer, repaymentReqDAO: FundRepaymentReqDAO)
  extends ProxyContractTxHandler(client, lendBoxExplorer, repaymentReqDAO) {
  private val logger: Logger = Logger(this.getClass)

  def handleReqs(): Unit = {
    logger.info("Handling Funding requests...")

    repaymentReqDAO.all.onComplete((requests => {
      requests.get.map(req => {
        try {
          if (req.ttl <= Time.currentTime || req.state == 2) {
            handleRemoval(req)
          } else {
            handleReq(req)
          }
        } catch {
          case e: Throwable => logger.error(StackTrace.getStackTraceStr(e))
        }
      })
    }))
  }

  def handleRemoval(req: FundRepaymentReq): Unit = {
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

  def handleReq(req: FundRepaymentReq): Unit = {
    try {
      val repaymentBox = lendBoxExplorer.getRepaymentBox(req.repaymentBoxId)

      if (isReady(req)) {
        try {
          fundRepaymentTx(req, repaymentBox)
        } catch {
          case e: Throwable => logger.info(s"funding failed for request ${req.repaymentTxID}")
        }
      }
    } catch {
      case _: Throwable =>
        logger.error(s"Error")
    }
  }

  def fundRepaymentTx(req: FundRepaymentReq, repaymentInputBox: InputBox): InputBox = {
    client.getClient.execute(ctx => {
      try {
        val paymentBoxList = getPaymentBoxes(req)

        val fundLendTx = SingleRepaymentTxFactory.createLenderFundRepaymentTx(repaymentInputBox, paymentBoxList.getBoxes.get(0), req)

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
}
