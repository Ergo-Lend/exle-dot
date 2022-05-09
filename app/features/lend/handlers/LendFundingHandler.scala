package features.lend.handlers

import node.Client
import common.{StackTrace, Time}
import ergo.{ErgCommons, TxState}
import errors.{connectionException, failedTxException, paymentNotCoveredException, proveException, skipException}
import config.Configs
import core.SingleLender.Ergs.LendBoxExplorer
import core.SingleLender.Ergs.boxes.SingleLenderLendBox
import core.SingleLender.Ergs.txs.{RefundProxyContractTx, SingleLenderTxFactory}
import io.persistence.doobs.dbHandlers.FundLendReqDAO
import io.persistence.doobs.models.FundLendReq
import org.ergoplatform.appkit.{Address, CoveringBoxes, InputBox}
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
    val lendBox = lendBoxExplorer.getLendBox(req.lendBoxId)
    val wrappedLendBox = new SingleLenderLendBox(lendBox)
    logger.info("removing request" + req.id)

    if (unSpentPaymentBoxes.nonEmpty) {
      try {
        val unSpentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, ErgCommons.InfiniteBoxValue)
        val covered = unSpentPaymentBoxes.getCoveredAmount >= req.ergAmount
        val deadlinePassed = client.getHeight > wrappedLendBox.fundingInfoRegister.deadlineHeight
        if (covered && !deadlinePassed) {
          logger.info(s"Request ${req.id} is going back to the request pool, creation fee is enough")
          fundLendReqDAO.updateTTL(req.id, Time.currentTime + Configs.creationDelay)
          throw skipException()
        } else {
          val refundTxId = refundProxyContract(req)
          if (refundTxId.nonEmpty) {
            fundLendReqDAO.deleteById(req.id)
            return
          }
        }
      } catch {
        case _: connectionException => throw new Throwable
        case _: failedTxException => throw new Throwable
        case e: skipException => logger.info(s"Skipping Exception: Lend funding, increasing ttl")
        case _: Throwable =>
          logger.error(s"Checking creation request ${req.id} failed")
      }
    } else {
      logger.info(s"will remove fund Lend request: ${req.id} with state: ${req.state}")
      fundLendReqDAO.deleteById(req.id)
      return
    }
  }

  def handleReq(req: FundLendReq): Unit = {
    try {
      val lendBox = lendBoxExplorer.getLendBox(req.lendBoxId)
      val wrappedLendBox = new SingleLenderLendBox(lendBox)

      if (isReady(req)) {
        val deadlinePassed = client.getHeight >= wrappedLendBox.fundingInfoRegister.deadlineHeight
        if (deadlinePassed) {
          refundProxyContract(req)
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
        val paymentBoxList = this.getFundPaymentBoxes(req, req.ergAmount).getBoxes.asScala

        val fundLendTx = SingleLenderTxFactory.createFundingLendBoxTx(lendingBox, paymentBoxList, req)
        val signedTx = fundLendTx.runTx(ctx)

        var fundTxId = ctx.sendTransaction(signedTx)

        if (fundTxId == null) throw failedTxException(s"fund lend tx sending failed for ${req.id}")
        else fundTxId = fundTxId.replaceAll("\"", "")

        fundLendReqDAO.updateLendTxId(req.id, fundTxId)
        fundLendReqDAO.updateStateById(req.id, TxState.Mined)
      } catch {
        case e: proveException => {
          fundLendReqDAO.updateStateById(req.id, TxState.ScriptFalsed)
          logger.error(s"Create Failure: Contract reduced to false")
          refundProxyContract(req)
        }
        case e: Throwable =>
          logger.error("Fund Failed")
          throw e
      }
    })
  }

  def refundProxyContract(req: FundLendReq): String = {
    client.getClient.execute(ctx => {
      try {
        val paymentBoxes = this.getFundPaymentBoxes(req, req.ergAmount).getBoxes.asScala
        val refundProxyContractTx = new RefundProxyContractTx(paymentBoxes, req.lenderAddress)

        val signedTx = refundProxyContractTx.runTx(ctx)

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

   def getFundPaymentBoxes(req: FundLendReq, amount: Long): CoveringBoxes = {
    val paymentAddress = Address.create(req.paymentAddress)
    val paymentBoxList = client.getCoveringBoxesFor(paymentAddress, amount)

    if (!paymentBoxList.isCovered)
      throw paymentNotCoveredException(
        s"FundLendReq: Payment for request ${req.id} not covered the fee, \n" +
          s"request state id ${req.state} and request tx is ${req.txId}.\n Payment address -> ${req.paymentAddress}.\n " +
          s"Amount to cover -> ${amount} \n " +
          s"Payment Amount from request -> ${req.ergAmount}")

    paymentBoxList
  }
}
