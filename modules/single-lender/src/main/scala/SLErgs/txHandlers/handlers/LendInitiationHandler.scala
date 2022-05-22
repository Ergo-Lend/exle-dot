package SLErgs.txHandlers.handlers

import SLErgs.LendBoxExplorer
import SLErgs.boxes.SLELendBox
import SLErgs.txs.{RefundProxyContractTx, SingleLenderTxFactory}
import commons.common.{StackTrace, Time}
import commons.TxState
import commons.configs.Configs
import commons.ergo.ErgCommons
import commons.errors._
import chain.explorer.Explorer
import commons.node.Client
import db.dbHandlers.{CreateLendReqDAO, DAO}
import db.models.{CreateLendReq, ProxyReq}
import org.ergoplatform.appkit.{Address, CoveringBoxes, InputBox, Parameters}
import play.api.Logger

import javax.inject.Inject
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.concurrent.ExecutionContext.Implicits.global

class LendInitiationHandler @Inject() (
  client: Client,
  lendBoxExplorer: LendBoxExplorer,
  createLendReqDAO: CreateLendReqDAO
) extends ProxyContractTxHandler(client, lendBoxExplorer, createLendReqDAO) {
  private val logger: Logger = Logger(this.getClass)

  def handleReqs(): Unit = {
    logger.info("Handling Creation requests...")

    createLendReqDAO.all.onComplete((requests => {
      requests.get.map { req =>
        try {
          logger.info(Time.currentTime.toString)
          if (req.ttl <= Time.currentTime || req.state == 2 || req.state == 4) {
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

  def handleRemoval(req: CreateLendReq): Unit = {
    val paymentAddress = Address.create(req.paymentAddress)
    val unSpentPaymentBoxes = client.getAllUnspentBox(paymentAddress)
    logger.info("removing request " + req.id)

    if (unSpentPaymentBoxes.nonEmpty) {
      try {
        val unSpentPaymentBoxes = client.getCoveringBoxesFor(
          paymentAddress,
          ErgCommons.InfiniteBoxValue
        )
        val covered =
          unSpentPaymentBoxes.getCoveredAmount >= SLELendBox.getLendBoxInitiationPayment
        val isScriptReducedToFalse = req.state == TxState.ScriptFalsed.id
        if (covered && !isScriptReducedToFalse) {
          logger.info(
            s"Request ${req.id} is going back to the request pool, creation fee is enough"
          )
          createLendReqDAO.updateTTL(
            req.id,
            Time.currentTime + Configs.creationDelay
          )
          throw SkipException()
        } else {
          val refundTxId = refundCreateLendProxy(req)
          if (refundTxId.nonEmpty) {
            createLendReqDAO.deleteById(req.id)
          }
        }
      } catch {
        case _: ConnectionException => throw new ConnectionException()
        case _: FailedTxException =>
          throw new FailedTxException("creation removal failed")
        case e: SkipException =>
          logger.info(s"Skipping removal: Request has enough funds")
        case _: Throwable =>
          logger.error(s"Checking creation request ${req.id} failed")
      }
    } else {
      logger.info(s"will remove request: ${req.id} with state: ${req.state}")
      createLendReqDAO.deleteById(req.id)
    }
  }

  def handleReq(req: CreateLendReq): Unit =
    try {
      if (isReady(req)) {
        createLendTx(req)
      }
    } catch {
      case e: Throwable => logger.error(e.getMessage)
    }

  def createLendTx(req: CreateLendReq): Unit =
    client.getClient.execute { ctx =>
      try {
        val lendServiceBoxInputBox: InputBox = lendBoxExplorer.getServiceBox
        val paymentBoxList = getPaymentBoxes(req).getBoxes.asScala

        val lendInitiationTx = SingleLenderTxFactory.createLendInitiationTx(
          lendServiceBoxInputBox,
          paymentBoxList,
          req
        )
        // Run the tx
        val signedTx = lendInitiationTx.runTx(ctx)

        // Sign it
        var createTxId = ctx.sendTransaction(signedTx)

        if (createTxId == null)
          throw FailedTxException(s"Creation tx sending failed for ${req.id}")
        else createTxId = createTxId.replaceAll("\"", "")

        createLendReqDAO.updateCreateTxID(req.id, createTxId)
        createLendReqDAO.updateStateById(req.id, TxState.Mined)
      } catch {
        case e: ProveException => {
          createLendReqDAO.updateStateById(req.id, TxState.ScriptFalsed)
          logger.error(s"Create Failure: Contract reduced to false")
          refundCreateLendProxy(req)
        }
        case _: Throwable => {

          logger.error(s"Create Failed for req ${req.id}")
        }
      }
    }

  def refundCreateLendProxy(req: CreateLendReq): String =
    client.getClient.execute { ctx =>
      val paymentBoxList = getPaymentBoxes(req).getBoxes.asScala
      val refundTx =
        new RefundProxyContractTx(paymentBoxList, req.paymentAddress)

      val signedTx = refundTx.runTx(ctx)

      val refundTxId = ctx.sendTransaction(signedTx)

      if (refundTxId == null)
        throw FailedTxException(s"Refund failed for ${req.paymentAddress}")
      return refundTxId
    }
}

class ProxyContractTxHandler @Inject() (
  client: Client,
  explorer: Explorer,
  dao: DAO
) {

  def getPaymentBoxes(
    req: ProxyReq,
    amount: Long = SLELendBox.getLendBoxInitiationPayment
  ): CoveringBoxes = {
    val paymentAddress = Address.create(req.paymentAddress)
    val paymentBoxList = client.getCoveringBoxesFor(paymentAddress, amount)

    if (!paymentBoxList.isCovered)
      throw PaymentNotCoveredException(
        s"Payment for request ${req.id} not covered the fee: \n" +
          s"request state id ${req.state} and request tx is ${req.txId}.\n Payment address: ${req.paymentAddress}.\n " +
          s"Amount to cover: ${amount} \n"
      )

    paymentBoxList
  }

  /**
    * Check to see if the transaction is ready to be run.
    * @param req
    * @return
    */
  def isReady(
    req: ProxyReq,
    coveringFee: Long = (Parameters.MinFee * 2)
  ): Boolean = {
    val paymentAddress = Address.create(req.paymentAddress)
    val coveringList = client.getCoveringBoxesFor(paymentAddress, coveringFee)
    if (coveringList.isCovered) {
      // If the box is created, give it time to be created
      dao.updateTTL(req.id, Time.currentTime + Configs.creationDelay)
    } else {
      // If the tx is in the mempool, give it some time
      val numberTxInMempool =
        explorer.getNumberTxInMempoolByAddress(req.paymentAddress)
      if (numberTxInMempool > 0) {
        dao.updateTTL(req.id, Time.currentTime + Configs.creationDelay)
      }
    }

    // if Req is not successful -> Run Tx (if covered)
    //    else if Req is successful
    //      -> Check the completed Tx
    //            if completed Tx == Mined, update state
    val reqTxState = TxState.apply(req.state)
    if (reqTxState == TxState.Unsuccessful) {
      if (coveringList.isCovered) return true
    } else if (reqTxState == TxState.Mined) {
      val txState = explorer.checkTransactionState(req.txId.getOrElse(""))
      if (txState == TxState.Mined)
        dao.updateStateById(req.id, TxState.Completed)
      else if (txState == TxState.Unsuccessful) return true
    }

    false
  }
}
