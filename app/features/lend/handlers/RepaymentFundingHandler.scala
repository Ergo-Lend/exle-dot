package features.lend.handlers

import node.Client
import common.{StackTrace, Time}
import ergo.{ErgCommons, TxState}
import errors.{
  connectionException,
  failedTxException,
  proveException,
  skipException
}
import config.Configs
import core.SingleLender.Ergs.LendBoxExplorer
import core.SingleLender.Ergs.txs.{
  RefundProxyContractTx,
  SingleRepaymentTxFactory
}
import io.persistence.doobs.dbHandlers.FundRepaymentReqDAO
import io.persistence.doobs.models.FundRepaymentReq
import org.ergoplatform.appkit.{Address, InputBox}
import play.api.Logger

import javax.inject.Inject
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.concurrent.ExecutionContext.Implicits.global

class RepaymentFundingHandler @Inject() (
  client: Client,
  lendBoxExplorer: LendBoxExplorer,
  repaymentReqDAO: FundRepaymentReqDAO
) extends ProxyContractTxHandler(client, lendBoxExplorer, repaymentReqDAO) {
  private val logger: Logger = Logger(this.getClass)

  def handleReqs(): Unit = {
    logger.info("Handling Funding requests...")

    repaymentReqDAO.all.onComplete((requests => {
      requests.get.map { req =>
        try {
          if (req.ttl <= Time.currentTime || req.state == 2) {
            handleRemoval(req)
          } else {
            handleReq(req)
          }
        } catch {
          case e: Throwable => logger.error(StackTrace.getStackTraceStr(e))
        }
      }
    }))
  }

  def handleRemoval(req: FundRepaymentReq): Unit = {
    val paymentAddress = Address.create(req.paymentAddress)
    val unSpentPaymentBoxes = client.getAllUnspentBox(paymentAddress)
    logger.info("removing request" + req.id)

    if (unSpentPaymentBoxes.nonEmpty) {
      try {
        val unSpentPaymentBoxes = client.getCoveringBoxesFor(
          paymentAddress,
          ErgCommons.InfiniteBoxValue
        )
        val covered = unSpentPaymentBoxes.getCoveredAmount >= req.ergAmount
        if (covered) {
          logger.info(
            s"Request ${req.id} is going back to the request pool, creation fee is enough"
          )
          repaymentReqDAO.updateTTL(
            req.id,
            Time.currentTime + Configs.creationDelay
          )
          throw skipException()
        } else {
          val refundTxId = refundProxyContract(req)
          if (refundTxId.nonEmpty) {
            repaymentReqDAO.deleteById(req.id)
          }
        }
      } catch {
        case _: connectionException => throw new Throwable
        case _: failedTxException   => throw new Throwable
        case e: skipException       => throw e
        case _: Throwable =>
          logger.error(s"Checking creation request ${req.id} failed")
      }
    } else {
      logger.info(
        s"Unspent Box empty: will remove fund repayment request: ${req.id} with state: ${req.state}"
      )
      repaymentReqDAO.deleteById(req.id)
    }
  }

  def handleReq(req: FundRepaymentReq): Unit =
    try {
      val repaymentBox = lendBoxExplorer.getRepaymentBox(req.repaymentBoxId)

      if (isReady(req)) {
        try {
          fundRepaymentTx(req, repaymentBox)
        } catch {
          case e: Throwable =>
            logger.info(s"funding failed for request ${req.repaymentTxID}")
            throw e
        }
      }
    } catch {
      case _: Throwable =>
        logger.error(s"Error")
    }

  def fundRepaymentTx(
    req: FundRepaymentReq,
    repaymentInputBox: InputBox
  ): Unit =
    client.getClient.execute { ctx =>
      try {
        val paymentBoxList =
          getPaymentBoxes(req, req.ergAmount).getBoxes.asScala

        val fundLendTx = SingleRepaymentTxFactory.createLenderFundRepaymentTx(
          repaymentInputBox,
          paymentBoxList,
          req
        )

        val signedTx = fundLendTx.runTx(ctx)

        var fundTxId = ctx.sendTransaction(signedTx)

        if (fundTxId == null)
          throw failedTxException(s"fund lend tx sending failed for ${req.id}")
        else fundTxId = fundTxId.replaceAll("\"", "")

        repaymentReqDAO.updateLendTxId(req.id, fundTxId)
        repaymentReqDAO.updateStateById(req.id, TxState.Mined)
      } catch {
        case e: proveException => {
          repaymentReqDAO.updateStateById(req.id, TxState.ScriptFalsed)
          logger.error(s"Create Failure: Contract reduced to false")
          refundProxyContract(req)
        }
        case _: Throwable => logger.error("Fund Failed")
      }
    }

  def refundProxyContract(req: FundRepaymentReq): String =
    client.getClient.execute { ctx =>
      try {
        val paymentBoxes = getPaymentBoxes(req, req.ergAmount).getBoxes.asScala
        val refundProxyContractTx =
          new RefundProxyContractTx(paymentBoxes, req.userAddress)

        val signedTx = refundProxyContractTx.runTx(ctx)

        var refundTxId = ctx.sendTransaction(signedTx)

        if (refundTxId == null)
          throw failedTxException(
            s"refund lend tx sending failed for ${req.id}"
          )
        else refundTxId = refundTxId.replaceAll("\"", "")

        refundTxId
      } catch {
        case e: Throwable =>
          logger.error("Fund Failed")
          throw e
      }
    }
}
