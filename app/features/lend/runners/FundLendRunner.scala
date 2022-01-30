package features.lend.runners

import ergotools.client.Client
import errors.failedTxException
import features.lend.LendBoxExplorer
import features.lend.boxes.SingleLenderLendBox
import features.lend.boxes.registers.SingleLenderRegister
import features.lend.contracts.proxyContracts.LendProxyContractService
import features.lend.runners.ExplorerRunner.{walletAddress, writeToFile}
import features.lend.txs.singleLender.{ProxyContractTx, SingleLenderTxFactory}
import org.ergoplatform.appkit.{Address, Parameters}

object FundLendRunner {
  val lendBoxId = "10a52f0f7ead2ca2e48e59ef505577079d62ea4b0675d261a3155213f49bf5e6"
  val paymentAddressString = "VLzgw4C2SAnzZqoEz3yoYM8GuiHp1UZ6HGejTEdHAE8nS1Xy1AXvUz6BVCpZhszLz14jxTb2bWquoGmiKnqokciRsyjKjamc3LVUjcKyVphN46CSsMtJbMKGZHtDsHQkm8L4sXHMgEFMjDmTpbAVopXGqsm3U4J4APCUf1ERVJqJRcZsNBcGnPdcwQwP4KofLf4EgAmin6HPgdAJBCgaxTb4QwdHgYrWXjXHKXaRLTprSJZit8qj9QC78DTJYTbPxA7m33Jb3h17DHvVnXNT3XTeo95zVSg87gMMfh1tEWGZnTuVCo631jcxgA9CJsyzZp1TsN3gyt8siUkit2THwbPPNzroVGJ6mq2MpQEE6v5Urbrc8vdmPN82bUZgNHU3Rbtpwp6hTJCo2tMDnnNypz2GozpUFMKJrPCAEJhvXWypyRqj"
  val lenderPk = "9f83nJY4x9QkHmeek6PJMcTrf2xcaHAT3j5HD5sANXibXjMUixn"
  val lendBoxFundPayment: Long = 12 * 1000 * 1000

  def createFundLendProxyContract(client: Client, explorer: LendBoxExplorer): String = {
    try {
      val lendBox = explorer.getLendBox(lendBoxId)
      val wrappedLendBox = new SingleLenderLendBox(lendBox)
      val lendProxyContractService = new LendProxyContractService(client)

      val paymentAddress = lendProxyContractService.getFundLendBoxProxyContract(
        lendBoxId,
        lenderPk)

      val amount = wrappedLendBox.getFundingTotalErgs()
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

  def handleFundLend(client: Client, explorer: LendBoxExplorer): Unit = {
    try {
      client.getClient.execute(ctx => {
        val lendBox = explorer.getLendBox(lendBoxId)
        val paymentAddress: Address = Address.create(paymentAddressString)

        System.out.println(s"Getting boxes for ${paymentAddressString}")

        val unspentPaymentBoxes = client.getUnspentBox(paymentAddress)
        System.out.println(s"${unspentPaymentBoxes.size} box found...")

        val singleLenderRegister = new SingleLenderRegister(walletAddress)
        val fundLendTx = SingleLenderTxFactory.createFundingLendBoxTx(
          lendBox,
          unspentPaymentBoxes(0),
          singleLenderRegister)

        val signedTransaction = fundLendTx.runTx(ctx)
        val fundTxId = ctx.sendTransaction(signedTransaction)

        if (fundTxId == null) throw failedTxException(s"Lend Fund failed for ${paymentAddressString}")

        val stringBuilder = new StringBuilder()
        stringBuilder.append(s"Create Tx ID: ${fundTxId} \n")
        stringBuilder.append(s"BoxId: ${signedTransaction.getOutputsToSpend.get(1).getId} \n")
        writeToFile("LendInitiation.txt", stringBuilder.toString())

        System.out.println(s"Create Tx ID: ${fundTxId}")
        System.out.println()
        System.out.println(signedTransaction)
        System.out.println()
        System.out.println(s"Box Id: ${signedTransaction.getOutputsToSpend.get(1).getId}")
      })
    } catch {
      case e: Exception => {
        throw e
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
          paymentAddress, lendBoxFundPayment).getBoxes

        System.out.println(s"${unspentPaymentBoxes.size()} box found...")

        val refundTx = new ProxyContractTx(unspentPaymentBoxes.get(0), walletAddress)

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
        return e.toString
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
