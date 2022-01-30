package features.lend.runners

import config.Configs
import ergotools.client.Client
import errors.failedTxException
import features.lend.LendBoxExplorer
import features.lend.boxes.SingleLenderRepaymentBox
import features.lend.boxes.registers.SingleAddressRegister
import features.lend.contracts.proxyContracts.LendProxyContractService
import features.lend.runners.ExplorerRunner.writeToFile
import features.lend.txs.singleLender.{ProxyContractTx, SingleLenderTxFactory, SingleRepaymentTxFactory}
import org.ergoplatform.appkit.{Address, Parameters}

object RepaymentRunner {
  val repaymentBoxId = ""
  val proxyPaymentAddress = ""
  val walletAddress = "9f83nJY4x9QkHmeek6PJMcTrf2xcaHAT3j5HD5sANXibXjMUixn"
  val repaymentBoxPaymentValue = 0

  def createFundRepaymentProxyContract(client: Client, explorer: LendBoxExplorer): Unit = {
    try {
      val repaymentBox = explorer.getRepaymentBox(repaymentBoxId)
      val wrappedRepaymentBox = new SingleLenderRepaymentBox(repaymentBox)

      val lendProxyContractService = new LendProxyContractService(client)

      val paymentAddress = lendProxyContractService.
        getFundRepaymentBoxProxyContract(
          repaymentBoxId,
          walletAddress)

      val fundAmount = wrappedRepaymentBox.getFullFundAmount

      val requiredPaymentInErgs: Float = fundAmount/Parameters.OneErg.toFloat

      val stringBuilder = new StringBuilder()
      stringBuilder.append(s"pk: ${walletAddress}\n")
      stringBuilder.append(s"\n")
      stringBuilder.append(s"paymentAddress: ${paymentAddress}\n")
      stringBuilder.append(s"paymentValue: ${requiredPaymentInErgs} Ergs\n")
      writeToFile("RepaymentProxyContract.txt", stringBuilder.toString())

      System.out.println(paymentAddress)
      System.out.println(s"Send value of: ${requiredPaymentInErgs} Ergs")

      paymentAddress
    } catch {
      case e: Exception => {
        System.out.println(e)
        return e.toString
      }
    }
  }

  def handleFundRepayment(client: Client, explorer: LendBoxExplorer): Unit = {
    try {
      client.getClient.execute(ctx => {
        val repaymentBox = explorer.getRepaymentBox(repaymentBoxId)

        val paymentAddress: Address = Address.create(proxyPaymentAddress)

        // get payment box
        val unspentPaymentBoxes = client.getUnspentBox(paymentAddress)

        System.out.println(s"Getting boxes for ${proxyPaymentAddress}")

        val singleAddressRegister = new SingleAddressRegister(walletAddress)
        val fundRepaymentTx = SingleRepaymentTxFactory.createLenderFundRepaymentTx(
          repaymentBox,
          unspentPaymentBoxes(0),
          singleAddressRegister)

        val signedTransaction = fundRepaymentTx.runTx(ctx)
        val fundTxId = ctx.sendTransaction(signedTransaction)

        if (fundTxId == null) throw failedTxException(s"Lend Fund failed for ${proxyPaymentAddress}")
        System.out.println(s"Create Tx ID: ${fundTxId}")
        System.out.println()
        System.out.println(signedTransaction)
        System.out.println()
        System.out.println(s"Box Id: ${signedTransaction.getOutputsToSpend.get(1).getId}")
      })
    } catch {
      case e: Exception => {
        System.out.println(e)
        return e.toString
      }
    }
  }

  // @todo change to repayment
  def repaymentSuccess(client: Client, explorer: LendBoxExplorer): Unit = {
    client.getClient.execute(ctx => {
      val serviceBox = explorer.getServiceBox
      val repaymentBox = explorer.getRepaymentBox(repaymentBoxId)

      val fundSuccessTx = SingleRepaymentTxFactory.createSingleLenderRepaymentFundedTx(serviceBox, repaymentBox)
      val signedTransaction = fundSuccessTx.runTx(ctx)

      val fundSuccessTxId = ctx.sendTransaction(signedTransaction)

      if (fundSuccessTxId == null) throw failedTxException(s"repayment Fund Success failed for ${repaymentBoxId}")
      System.out.println(s"Create Tx ID: ${fundSuccessTxId}")
      System.out.println()
      System.out.println(signedTransaction)
      System.out.println()
      System.out.println(s"Box Id: ${signedTransaction.getOutputsToSpend.get(1).getId}")
    })
  }

  def handleRefundProxy(client: Client): Unit = {
    client.getClient.execute(ctx => {
      val paymentAddress: Address = Address.create(proxyPaymentAddress)

      System.out.println(s"Getting boxes for ${proxyPaymentAddress}")

      // get payment box
      val unspentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, repaymentBoxPaymentValue).getBoxes

      System.out.println(s"${unspentPaymentBoxes.size()} box found...")

      val refundTx = new ProxyContractTx(unspentPaymentBoxes.get(0), walletAddress)

      val signedTx = refundTx.runTx(ctx)

      var refundTxId = ctx.sendTransaction(signedTx)

      if (refundTxId == null) throw failedTxException(s"Refund failed for ${proxyPaymentAddress}")

      val stringBuilder = new StringBuilder()
      stringBuilder.append(s"Repayment Refund Tx ID: ${refundTxId} \n")
      stringBuilder.append(s"BoxId: ${signedTx.getOutputsToSpend.get(1).getId} \n")
      writeToFile("RepaymentProxyRefund.txt", stringBuilder.toString())

      System.out.println(s"Refund Tx ID: ${refundTxId}")
      System.out.println()
      System.out.println(signedTx)
      System.out.println()
      System.out.println(s"Box Id: ${signedTx.getOutputsToSpend.get(1).getId}")
    })
  }
}
