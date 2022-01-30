package features.lend.handlers

import config.Configs
import ergotools.TxState
import ergotools.client.Client
import errors.failedTxException
import features.lend.LendBoxExplorer
import features.lend.boxes.{SingleLenderLendBox}
import features.lend.dao.{FundLendReq, FundLendReqDAO}
import features.lend.txs.singleLender.SingleLenderTxFactory
import helpers.{StackTrace, Time}
import org.ergoplatform.appkit.{Address, InputBox}
import play.api.Logger

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global

class LendFundingHandler @Inject()(client: Client, lendBoxExplorer: LendBoxExplorer, fundLendReqDAO: FundLendReqDAO)
  extends ProxyContractTxHandler(client, lendBoxExplorer, fundLendReqDAO) {
  private val logger: Logger = Logger(this.getClass)

  def handleReqs(): Unit = {
    logger.info("Handling Funding requests...")

    fundLendReqDAO.all.onComplete((requests => {
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

  def handleRemoval(req: FundLendReq): Unit = {
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
        case e: Throwable => logger.info("funding removal failed")
      }
    }
  }

  def handleReq(req: FundLendReq): Unit = {
    try {
      val lendBox = lendBoxExplorer.getLendBox(req.lendBoxId)
      val wrappedLendBox = new SingleLenderLendBox(lendBox)

      if (isReady(req)) {
        val deadlinePassed = client.getHeight >= wrappedLendBox.fundingInfoRegister.deadlineHeight
        if (deadlinePassed) {
          refundBox(req)
        } else {
          try {
            fundLendTx(req, lendBox)
          } catch {
            case e: Throwable => logger.info(s"funding failed for request ${req.lendBoxId}")
          }
        }
      }
    } catch {
      case _: Throwable =>
        logger.error(s"Error")
    }
  }

  def fundLendTx(req: FundLendReq, lendingBox: InputBox): InputBox = {
    client.getClient.execute(ctx => {
      try {
        val paymentBoxList = getPaymentBoxes(req).getBoxes

        val fundLendTx = SingleLenderTxFactory.createFundingLendBoxTx(lendingBox, paymentBoxList.get(0), req)
        val signedTx = fundLendTx.runTx(ctx)

        var fundTxId = ctx.sendTransaction(signedTx)

        if (fundTxId == null) throw failedTxException(s"fund lend tx sending failed for ${req.id}")
        else fundTxId = fundTxId.replaceAll("\"", "")

        fundLendReqDAO.updateLendTxId(req.id, fundTxId)
        fundLendReqDAO.updateStateById(req.id, TxState.Mined)

        signedTx.getOutputsToSpend.get(0)
      } catch {
        case _: Throwable => logger.error("Fund Failed")
          lendingBox
      }
    })
  }

  def refundBox(req: FundLendReq): Unit = {
    client.getClient.execute(ctx => {
      try {
        val lendBox = lendBoxExplorer.getLendBox(req.lendBoxId)
        val serviceBox = lendBoxExplorer.getServiceBox

        val refundLendBoxTx = SingleLenderTxFactory.createRefundLendBoxTx(serviceBox, lendBox)

        val signedTx = refundLendBoxTx.runTx(ctx)

        var refundTxId = ctx.sendTransaction(signedTx)

        if (refundTxId == null) throw failedTxException(s"refund lend tx sending failed for ${req.id}")
        else refundTxId = refundTxId.replaceAll("\"", "")

        fundLendReqDAO.deleteById(req.id)
      } catch {
        case _: Throwable => logger.error("Fund Failed")
      }
    })
  }

  def isReady(req: FundLendReq): Boolean = {
    val paymentAddress = Address.create(req.paymentAddress)
    val coveringList = client.getCoveringBoxesFor(paymentAddress, 4 * Configs.fee)
    if (coveringList.isCovered) {
      fundLendReqDAO.updateTTL(req.id, Time.currentTime + Configs.creationDelay)
    } else {
      val numberTxInMempool = lendBoxExplorer.getNumberTxInMempoolByAddress(req.paymentAddress)
      if (numberTxInMempool > 0) {
        fundLendReqDAO.updateTTL(req.id, Time.currentTime + Configs.creationDelay)
      }
    }

    val reqTxState = TxState.apply(req.state)
    if (reqTxState == TxState.Unsuccessful) {
      if (coveringList.isCovered) return true
    } else if (reqTxState == TxState.Mined) {
      val txState = lendBoxExplorer.checkTransaction(req.lendTxID.getOrElse(""))
      if (txState == TxState.Mined)
        fundLendReqDAO.updateStateById(req.id, TxState.Mempooled)
      else if (txState == TxState.Unsuccessful) return true
    }

    false
  }
}
