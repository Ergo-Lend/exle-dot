package features.lend.handlers

import config.Configs
import ergotools.TxState
import ergotools.client.Client
import errors.{connectionException, failedTxException, skipException}
import features.lend.LendBoxExplorer
import features.lend.boxes.SingleLenderLendBox
import features.lend.dao.{FundLendReq, FundLendReqDAO}
import features.lend.txs.singleLender.SingleLenderTxFactory
import helpers.{StackTrace, Time}
import org.ergoplatform.appkit.{Address, InputBox}
import play.api.Logger

import javax.inject.Inject
import scala.collection.JavaConverters.asScalaBufferConverter
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
        if (unSpentPaymentBoxes.getCoveredAmount >= SingleLenderLendBox.getLendBoxInitiationPayment) {
          logger.info(s"Request ${req.id} is going back to the request pool, creation fee is enough")
          fundLendReqDAO.updateTTL(req.id, Time.currentTime + Configs.creationDelay)
          throw skipException()
        } else {
          val refundTxId = refundBox(req)
          if (refundTxId.nonEmpty) {
            fundLendReqDAO.deleteById(req.id)
          }
        }
      } catch {
        case _: connectionException => throw new Throwable
        case _: failedTxException => throw new Throwable
        case e: skipException => throw e
        case _: Throwable =>
          logger.error(s"Checking creation request ${req.id} failed")
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
            case e: Throwable =>
              logger.info(s"funding failed for request ${req.lendBoxId}")
              throw e
          }
        }
      }
    } catch {
      case e: Throwable =>
        logger.error(s"Error")
        throw e
    }
  }

  def fundLendTx(req: FundLendReq, lendingBox: InputBox): Unit = {
    client.getClient.execute(ctx => {
      try {
        val paymentBoxList = getPaymentBoxes(req).getBoxes.asScala

        val fundLendTx = SingleLenderTxFactory.createFundingLendBoxTx(lendingBox, paymentBoxList, req)
        val signedTx = fundLendTx.runTx(ctx)

        var fundTxId = ctx.sendTransaction(signedTx)

        if (fundTxId == null) throw failedTxException(s"fund lend tx sending failed for ${req.id}")
        else fundTxId = fundTxId.replaceAll("\"", "")

        fundLendReqDAO.updateLendTxId(req.id, fundTxId)
        fundLendReqDAO.updateStateById(req.id, TxState.Mined)
      } catch {
        case e: Throwable =>
          logger.error("Fund Failed")
          throw e
      }
    })
  }

  def refundBox(req: FundLendReq): String = {
    client.getClient.execute(ctx => {
      try {
        val lendBox = lendBoxExplorer.getLendBox(req.lendBoxId)
        val serviceBox = lendBoxExplorer.getServiceBox

        val refundLendBoxTx = SingleLenderTxFactory.createRefundLendBoxTx(serviceBox, lendBox)

        val signedTx = refundLendBoxTx.runTx(ctx)

        var refundTxId = ctx.sendTransaction(signedTx)

        if (refundTxId == null) throw failedTxException(s"refund lend tx sending failed for ${req.id}")
        else refundTxId = refundTxId.replaceAll("\"", "")

        refundTxId
      } catch {
        case e: Throwable =>
          logger.error("Fund Failed")
          throw e
      }
    })
  }
}
