package features.lend.runners

import ergotools.client.Client
import errors.failedTxException
import features.lend.LendBoxExplorer
import features.lend.boxes.SingleLenderLendBox
import features.lend.boxes.registers.SingleLenderRegister
import features.lend.contracts.proxyContracts.LendProxyContractService
import features.lend.runners.ExplorerRunner.{walletAddress, writeToFile}
import features.lend.txs.singleLender.{RefundProxyContractTx, SingleLenderTxFactory}
import org.ergoplatform.appkit.{Address, ErgoContract, Parameters, SignedTransaction}

import scala.collection.JavaConverters.asScalaBufferConverter

case class FundLendRunner(lendBoxId: String, lenderAddress: String, explorer: LendBoxExplorer) {
  var paymentAddress: Address = Address.create("25JQJ8QcBRwjjm1C46MPfEoA9LuMDJwFGtFenRAG1rdaANCRqA1B7yKtwg3Q2GaTQKTRgfm7aAqhWFsvf4RNovGxUAi8DFMsPCe2UhX6qu5NaoSnKzLYWE5Epz4Bdx13oEznraTxfiKmQ5tau4g6g4K7h3RpCDrjUmcHxP1u1TH2jEqF6b7dxRFtfFKfe6jH4RQrV6Ypyg1K4mao2T2yA614x14RDvuHu7QUY1HzoB9HA1WyCLifLzzysUGzc9esohMRXTMRPkqkQzMfXTfbyFtSvcXRy28Yhwh4bjdhdwbjjfYRKWXY1K34PDfH1wtPp796M5yxWUfVsEenk9y7vSQ7wFoTRar2FVUoWPYtx1JYaYzZrAbECjYEDRNgrHc1E9SVZKvSeVy45Z2xLekMQFTPfjAiwciXLLFf8mL4iVhqi8TrX5hujVrEh")
  def proxyContract(client: Client, sendFunds: (ErgoContract, Long) => SignedTransaction): SignedTransaction = {
    val lendBox = explorer.getLendBox(lendBoxId)
    val wrappedLendBox = new SingleLenderLendBox(lendBox)

    val lendProxyContractService = new LendProxyContractService(client)
    val paymentAddressContract = lendProxyContractService.getFundLendBoxProxyContract(
      lendId = lendBoxId,
      lenderAddress = lenderAddress
    )

    val paymentAddressString = lendProxyContractService.getFundLendBoxProxyContractString(
      lendId = lendBoxId,
      lenderAddress = lenderAddress
    )

    paymentAddress = Address.create(paymentAddressString)
    val totalAmountToFund = wrappedLendBox.getFundingTotalErgs

    val stringBuilder = new StringBuilder()
    stringBuilder.append(s"pk: ${walletAddress}\n")
    stringBuilder.append(s"boxId: ${lendBoxId}\n")
    stringBuilder.append(s"\n")
    stringBuilder.append(s"paymentAddress: ${paymentAddress}\n")
    stringBuilder.append(s"paymentValue: ${totalAmountToFund/Parameters.OneErg} Ergs\n")
    writeToFile("FundLendReq.txt", stringBuilder.toString())

    sendFunds(paymentAddressContract, wrappedLendBox.getFundingTotalErgs)
  }

  def handleProxyMerge(client: Client, explorer: LendBoxExplorer): Unit = {
    val lendBox = explorer.getLendBox(lendBoxId)
    val wrappedLendBox = new SingleLenderLendBox(lendBox)
    System.out.println(s"Getting boxes for ${paymentAddress.toString}")

    val unspentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, wrappedLendBox.getFundingTotalErgs).getBoxes.asScala

    System.out.println(s"${unspentPaymentBoxes.size} box found...")

    val singleLenderRegister = new SingleLenderRegister(walletAddress)
    val fundLendTx = SingleLenderTxFactory.createFundingLendBoxTx(
      lendBox,
      unspentPaymentBoxes,
      singleLenderRegister)

    client.getClient.execute(ctx => {
      val signedTransaction = fundLendTx.runTx(ctx)
      val fundTxId = ctx.sendTransaction(signedTransaction)

      System.out.println(signedTransaction.toJson(true))
      if (fundTxId == null) throw failedTxException(s"FundLend failed for ${paymentAddress.toString}")
    })
  }

  def handleSuccess(client: Client, explorer: LendBoxExplorer): Unit = {
    val serviceBox = explorer.getServiceBox
    val lendingBox = explorer.getLendBox(lendBoxId)

    val fundSuccessTx = SingleLenderTxFactory.createFundedLendBoxTx(serviceBox, lendingBox)

    client.getClient.execute(ctx => {
      val signedTransaction = fundSuccessTx.runTx(ctx)

      val fundSuccessTxId = ctx.sendTransaction(signedTransaction)

      if (fundSuccessTxId == null) throw failedTxException(s"lendFund Success failed for ${lendBoxId}")
      System.out.println(s"Lend Fund Success Tx ID: ${fundSuccessTxId}")
      System.out.println()
      System.out.println(signedTransaction)
      System.out.println()
      System.out.println(s"Repayment Box Id: ${signedTransaction.getOutputsToSpend.get(1).getId}")
    })
  }
}

object FundLendRunner {
  val lendBoxId: String = "41b38d7879f2d016aa74b5250545e2a6ab1f0d8513b92a551d59edeaac1d0089"
  val paymentAddressString: String = "S4zcopbihvfXvFPWQHBz5jxeRVosPDtPtP6W7xptmX2C9u4QxkdKTv2Uv9NrKHQeeevBBrDPSi2GQMhgxALRc2hy98CJxS8MrniJPrn3BnvnJhQg2pNQWYwfCVeybdWXWLY7D9UR6bkwDoLBTCWjhQgo3uCs9AXTCThzf8CS4i3M2CNSsxDfA7UzFVzTCEvSa8UAyCRiuVRFQGQ5Dv437YU1UUdh1xXfFnaMwHGyDxzcFr66gZHqBcKfVjBPNDP3XJ1yNffGMQE1kRmBJ4bS2NCyPJtaviyxcnZciUigpMaFoQvnYmscmZvPX4kzcJ7wexnath99JjWvmYtAhPrLjqYK5JF9qFG3gm1pwwzEEupsKMQEqHLr5Cqa1s6axiZ6RRkW84JHgRGXcUFSdQgxmipftXYgMusrCs5CRVXqWeX8ZTc6urrr3BVWb6xtCMHuAoqsbAWwKLrvQy92Y1zPm3fBD"
  val lenderPk: String = "9f83nJY4x9QkHmeek6PJMcTrf2xcaHAT3j5HD5sANXibXjMUixn"
  val lendBoxFundPayment: Long = 12 * 1000 * 1000

  def createFundLendProxyContract(client: Client, explorer: LendBoxExplorer): String = {
    try {
      val lendBox = explorer.getLendBox(lendBoxId)
      val wrappedLendBox = new SingleLenderLendBox(lendBox)
      val lendProxyContractService = new LendProxyContractService(client)

      val paymentAddress = lendProxyContractService.getFundLendBoxProxyContractString(
        lendBoxId,
        lenderPk)

      val amount = wrappedLendBox.getFundingTotalErgs
      val requiredPaymentInErgs: Float = amount/Parameters.OneErg.toFloat

      val stringBuilder = new StringBuilder()
      stringBuilder.append(s"pk: ${walletAddress}\n")
      stringBuilder.append(s"boxId: ${lendBoxId}\n")
      stringBuilder.append(s"\n")
      stringBuilder.append(s"paymentAddress: ${paymentAddress}\n")
      stringBuilder.append(s"paymentValue: ${requiredPaymentInErgs} Ergs\n")
      writeToFile("FundLendReq.txt", stringBuilder.toString())

      System.out.println(paymentAddress)
      System.out.println(s"Send value of: ${requiredPaymentInErgs} Ergs")
      System.out.println(s"Send ${requiredPaymentInErgs} to ${paymentAddress}")

      paymentAddress
    } catch {
      case e: Exception => {
        System.out.println(e)
        return e.toString
      }
    }
  }

  def handleRefundProxy(client: Client): Unit = {
    try {
      client.getClient.execute(ctx => {
        val paymentAddress: Address = Address.create(paymentAddressString)

        System.out.println(s"Getting boxes for ${paymentAddressString}")

        // get payment box
        val unspentPaymentBoxes = client.getCoveringBoxesFor(
          paymentAddress, lendBoxFundPayment).getBoxes.asScala

        System.out.println(s"${unspentPaymentBoxes.size} box found...")

        val refundTx = new RefundProxyContractTx(unspentPaymentBoxes, walletAddress)

        val signedTx = refundTx.runTx(ctx)

        var refundTxId = ctx.sendTransaction(signedTx)

        if (refundTxId == null) throw failedTxException(s"Refund failed for ${paymentAddressString}")

        val stringBuilder = new StringBuilder()
        stringBuilder.append(s"LendInitiation Refund Tx ID: ${refundTxId} \n")
        stringBuilder.append(s"BoxId: ${signedTx.getOutputsToSpend.get(1).getId} \n")
        writeToFile("LendInitiationRefund.txt", stringBuilder.toString())

        System.out.println(s"Refund Tx ID: ${refundTxId}")
        System.out.println()
        System.out.println(signedTx)
        System.out.println()
        System.out.println(s"Box Id: ${signedTx.getOutputsToSpend.get(1).getId}")
      })
    } catch {
      case e: Exception => {
        System.out.println(e)
        throw e
      }
    }
  }

  def lendFundSuccess(client: Client, explorer: LendBoxExplorer): Unit = {
    try {
      client.getClient.execute(ctx => {
        val serviceBox = explorer.getServiceBox
        val lendingBox = explorer.getLendBox(lendBoxId)

        val fundSuccessTx = SingleLenderTxFactory.createFundedLendBoxTx(serviceBox, lendingBox)
        val signedTransaction = fundSuccessTx.runTx(ctx)

        val fundSuccessTxId = ctx.sendTransaction(signedTransaction)

        if (fundSuccessTxId == null) throw failedTxException(s"lendFund Success failed for ${lendBoxId}")
        System.out.println(s"Lend Fund Success Tx ID: ${fundSuccessTxId}")
        System.out.println()
        System.out.println(signedTransaction)
        System.out.println()
        System.out.println(s"Repayment Box Id: ${signedTransaction.getOutputsToSpend.get(1).getId}")
      })
    } catch {
      case e: Exception => {
        throw e
      }
    }
  }

  def refundLend(client: Client, explorer: LendBoxExplorer): Unit = {
    client.getClient.execute(ctx => {
      try {
        val lendBox = explorer.getLendBox(lendBoxId)
        val serviceBox = explorer.getServiceBox

        val refundLendBoxTx = SingleLenderTxFactory.createRefundLendBoxTx(serviceBox, lendBox)

        val signedTx = refundLendBoxTx.runTx(ctx)

        var refundTxId = ctx.sendTransaction(signedTx)

        if (refundTxId == null) throw failedTxException(s"refund lend tx sending failed for ${lendBoxId}")
        else refundTxId = refundTxId.replaceAll("\"", "")

      } catch {
        case _: Throwable => throw failedTxException("Fund Failed")
      }
    })
  }
}
