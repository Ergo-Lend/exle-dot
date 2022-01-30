package features.lend.handlers

import config.Configs
import ergotools.TxState
import ergotools.client.Client
import ergotools.explorer.Explorer
import errors.{connectionException, failedTxException, paymentNotCoveredException, skipException}
import features.lend.LendBoxExplorer
import features.lend.dao.{CreateLendReq, CreateLendReqDAO, DAO, ProxyReq}
import features.lend.txs.singleLender.SingleLenderTxFactory
import helpers.{StackTrace, Time}
import org.ergoplatform.appkit.{Address, CoveringBoxes, InputBox}
import play.api.Logger

import javax.inject.Inject
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.concurrent.ExecutionContext.Implicits.global

class LendInitiationHandler @Inject()(client: Client, lendBoxExplorer: LendBoxExplorer, createLendReqDAO: CreateLendReqDAO)
  extends ProxyContractTxHandler(client, lendBoxExplorer, createLendReqDAO) {
  private val logger: Logger = Logger(this.getClass)

  def handleReqs(): Unit = {
    logger.info("Handling Creation requests...")

    createLendReqDAO.all.onComplete((requests => {
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
    })
    )
  }

  //@todo finish removals
  def handleRemoval(req: CreateLendReq): Unit = {
    val paymentAddress = Address.create(req.paymentAddress)
    val unSpentPaymentBoxes = client.getAllUnspentBox(paymentAddress)
    logger.info("removing request " + req.id)

    if (unSpentPaymentBoxes.nonEmpty) {
      try {
        val unSpentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, Configs.infBoxVal)
        if (unSpentPaymentBoxes.getCoveredAmount >= Configs.fee * 4) {
          logger.info(s"Request ${req.id} is going back to the request pool, creation fee is enough")
          createLendReqDAO.updateTTL(req.id, Time.currentTime + Configs.creationDelay)
          throw skipException()
        }
      } catch {
        case _: connectionException => throw new Throwable
        case _: failedTxException => throw new Throwable
        case e: skipException => throw e
        case _: Throwable => logger.error(s"Checking creation request ${req.id} failed")
      }
    }
    else {
      logger.info(s"will remove request: ${req.id} with state: ${req.state}")
      createLendReqDAO.deleteById(req.id)
    }
  }

  def handleReq(req: CreateLendReq): Unit = {
    try {
      if (isReady(req)) {
        createLendTx(req)
      }
    } catch {
      case _: Throwable => logger.error(s"Error")
    }
  }

  def createLendTx(req: CreateLendReq): Unit = {
    client.getClient.execute(ctx => {
      try {
        val lendServiceBoxInputBox: InputBox = lendBoxExplorer.getServiceBox
        val paymentBoxList = getPaymentBoxes(req).getBoxes

        val lendInitiationTx = SingleLenderTxFactory.createLendInitiationTx(lendServiceBoxInputBox, paymentBoxList.get(0), req)
        // Run the tx
        val signedTx = lendInitiationTx.runTx(ctx)

        // Sign it
        var createTxId = ctx.sendTransaction(signedTx)

        if (createTxId == null) throw failedTxException(s"Creation tx sending failed for ${req.id}")
        else createTxId = createTxId.replaceAll("\"", "")

        createLendReqDAO.updateCreateTxID(req.id, createTxId)
        createLendReqDAO.updateStateById(req.id, TxState.Mined)
      } catch {
        case _: Throwable => logger.error("Create Failed")
      }
    })
  }
}

class ProxyContractTxHandler @Inject()(client: Client, explorer: Explorer, dao: DAO) {
  def getPaymentBoxes(req: ProxyReq): CoveringBoxes = {
    val paymentAddress = Address.create(req.paymentAddress)
    val paymentBoxList = client.getCoveringBoxesFor(paymentAddress, Configs.fee * 4)

    if (!paymentBoxList.isCovered)
      throw paymentNotCoveredException(s"Payment for request ${req.id} not covered the fee, request state id ${req.state} and request tx is ${req.txId}")

    paymentBoxList
  }

  /**
   * Check to see if the transaction is ready to be run.
   * This involves checking if the
   * @param req
   * @return
   */
  def isReady(req: ProxyReq): Boolean = {
    val paymentAddress = Address.create(req.paymentAddress)
    val coveringList = client.getCoveringBoxesFor(paymentAddress, 4 * Configs.fee)
    if (coveringList.isCovered) {
      dao.updateTTL(req.id, Time.currentTime + Configs.creationDelay)
    } else {
      val numberTxInMempool = explorer.getNumberTxInMempoolByAddress(req.paymentAddress)
      if (numberTxInMempool > 0) {
        dao.updateTTL(req.id, Time.currentTime + Configs.creationDelay)
      }
    }

    val reqTxState = TxState.apply(req.state)
    if (reqTxState == TxState.Unsuccessful) {
      if (coveringList.isCovered) return true
    } else if (reqTxState == TxState.Mined) {
      val txState = explorer.checkTransaction(req.txId.getOrElse(""))
      if (txState == TxState.Mined)
        dao.updateStateById(req.id, TxState.Mempooled)
      else if (txState == TxState.Unsuccessful) return true
    }

    false
  }
}
