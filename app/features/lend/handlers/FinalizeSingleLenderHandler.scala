package features.lend.handlers

import config.Configs
import ergotools.client.Client
import errors.{connectionException, failedTxException}
import features.lend.LendBoxExplorer
import features.lend.boxes.{SingleLenderLendBox, SingleLenderLendBoxContract, SingleLenderRepaymentBox, SingleLenderRepaymentBoxContract}
import features.lend.txs.singleLender.{SingleLenderTxFactory, SingleRepaymentTxFactory}
import helpers.StackTrace
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoClientException, InputBox}
import play.api.Logger

import javax.inject.Inject

class FinalizeSingleLenderHandler @Inject()(client: Client, explorer: LendBoxExplorer) {
  private val logger: Logger = Logger(this.getClass)

  def handleReqs(): Unit = {
    logger.info("Handling finalize process...")
    try {
      processRequests()
    } catch {
      case e: ErgoClientException => logger.warn(e.getMessage)
      case e: Throwable => logger.error(StackTrace.getStackTraceStr(e))
    }
  }

  def processRequests(): Unit = {
    client.getClient.execute((ctx: BlockchainContext) => {
//      processFundedLendBoxes(ctx)
//      processFundedRepaymentBoxes(ctx)
      processRefundLendBoxes(ctx)
    })
  }

  /**
   *
   * Input Boxes:
   * - ServiceBox
   * - FundedLendBox
   *
   * OutputBoxes:
   * - ServiceBox
   * - RepaymentBox
   * - Funds to Borrower
   * @param ctx
   */
  def processFundedLendBoxes(ctx: BlockchainContext): Unit = {
    try {
      val lendBoxContract = SingleLenderLendBoxContract.getContract(ctx).getErgoTree
      val encodedAddress = Configs.addressEncoder.fromProposition(lendBoxContract).get.toString

      client.getAllUnspentBox(Address.create(encodedAddress))
        .filter(box => {
          val wrappedBox = new SingleLenderLendBox(box)
          val isFunded = box.getValue >= wrappedBox.fundingInfoRegister.fundingGoal

          isFunded
        }).foreach(lendBox => {
        if (!explorer.isBoxInMemPool(lendBox)) processFundedLendBox(ctx, lendBox)
      })
    } catch {
      case e: connectionException => logger.warn(e.getMessage)
      case e: Throwable => logger.error(StackTrace.getStackTraceStr(e))
    }
  }

  def processFundedLendBox(ctx: BlockchainContext, lendBox: InputBox): Unit = {
    try {
      val wrappedLendBox = new SingleLenderLendBox(lendBox)

      val fundingSuccess = wrappedLendBox.value >= wrappedLendBox.fundingInfoRegister.fundingGoal
      if (fundingSuccess) {
        val serviceBox = explorer.getServiceBox
        val fundedLendTx = SingleLenderTxFactory.createFundedLendBoxTx(serviceBox, lendBox)
        val signedTx = fundedLendTx.runTx(ctx)

        val fundedTxId = ctx.sendTransaction(signedTx)

        if (fundedTxId == null) throw failedTxException(s"funded lendbox sending failed for ${wrappedLendBox.id.toString}")
      }
    } catch {
      case e: Throwable =>
        logger.error("LendBox Funded Failed")
        throw e
    }
  }

  /**
   *
   * Input Boxes:
   * - ServiceBox
   * - RepaymentBox
   *
   * OutputBoxes:
   * - ServiceBox + 1 LendToken
   * - Funds to ErgoLendTeam
   * - Funds to Lender
   * @param ctx
   */
  def processFundedRepaymentBoxes(ctx: BlockchainContext): Unit = {
    try {
      val repaymentBoxContract = SingleLenderRepaymentBoxContract.getContract(ctx).getErgoTree
      val encodedAddress = Configs.addressEncoder.fromProposition(repaymentBoxContract).get.toString

      client.getAllUnspentBox(Address.create(encodedAddress))
        .filter(box => {
          val wrappedBox = new SingleLenderRepaymentBox(box)
          val isRepaid = box.getValue >= wrappedBox.repaymentDetailsRegister.repaymentAmount
          isRepaid
        }).foreach(repaymentBox => {
          if (!explorer.isBoxInMemPool(repaymentBox)) processFundedRepaymentBox(ctx, repaymentBox)
      })
    } catch {
      case e: connectionException => logger.warn(e.getMessage)
      case e: Throwable => logger.error(StackTrace.getStackTraceStr(e))
    }


  }

  def processFundedRepaymentBox(ctx: BlockchainContext, repaymentBox: InputBox): Unit = {
    try {
      val wrappedRepaymentBox = new SingleLenderRepaymentBox(repaymentBox)
      val isFunded = wrappedRepaymentBox.value >= wrappedRepaymentBox.repaymentDetailsRegister.repaymentAmount

      if (isFunded) {
        val serviceBox = explorer.getServiceBox
        val repaymentFundedTx = SingleRepaymentTxFactory.createSingleLenderRepaymentFundedTx(serviceBox, repaymentBox)

        val signedTx = repaymentFundedTx.runTx(ctx)
        val repaymentFundedTxId = ctx.sendTransaction(signedTx)

        if (repaymentFundedTxId == null) throw failedTxException(s"Repayment funded failed for ${wrappedRepaymentBox.id.toString}")
      }
    } catch {
      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
    }
  }

  /**
   * ## This method should be called
   *
   * InputBox:
   * - ServiceBox
   * - LendingBox
   *
   * OutputBox:
   * - ServiceBox + 1 Token
   * - Funds to Lender
   * @param ctx
   */
  def processRefundLendBoxes(ctx: BlockchainContext): Unit = {
    try {
      val lendBoxContract = SingleLenderLendBoxContract.getContract(ctx).getErgoTree
      val encodedAddress = Configs.addressEncoder.fromProposition(lendBoxContract).get.toString

      client.getAllUnspentBox(Address.create(encodedAddress))
        .filter(box => {
          val wrappedBox = new SingleLenderLendBox(box)
          val isFunded = wrappedBox.value < wrappedBox.fundingInfoRegister.fundingGoal
          val isPastDeadline = client.getHeight > wrappedBox.fundingInfoRegister.deadlineHeight

          val shouldRefund = !isFunded && isPastDeadline

          shouldRefund
        }).foreach(lendBox => {
          if (!explorer.isBoxInMemPool(lendBox)) processRefundLendBox(ctx, lendBox)
        })
    } catch {
      case e: connectionException => logger.warn(e.getMessage)
      case e: Throwable => logger.error(StackTrace.getStackTraceStr(e))
    }
  }

  def processRefundLendBox(ctx: BlockchainContext, lendBox: InputBox): Unit = {

    // Get ServiceBox
    // Get LendingBox that are funded

    // Run Tx
    // Output Box should be the same
    val serviceBox = explorer.getServiceBox

    try {
      val refundLendBoxTx = SingleLenderTxFactory.createRefundLendBoxTx(serviceBox, lendBox)
      val signedTx = refundLendBoxTx.runTx(ctx)

      val signedTxId = ctx.sendTransaction(signedTx)

      if (!isTxPassed(signedTxId)) throw failedTxException(s"refund Transaction failed for ${lendBox.getId}")
    } catch {
      case e: Throwable => throw e
    }
  }

  def isTxPassed(txId: String): Boolean = {
    txId != null
  }
}
