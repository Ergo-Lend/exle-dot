package features.lend.runners

import ergotools.client.Client
import errors.failedTxException
import features.lend.LendBoxExplorer
import features.lend.boxes.SingleLenderRepaymentBox
import features.lend.boxes.registers.{SingleAddressRegister, SingleLenderRegister}
import features.lend.contracts.proxyContracts.LendProxyContractService
import features.lend.runners.ExplorerRunner.{walletAddress, writeToFile}
import features.lend.txs.singleLender.{RefundProxyContractTx, SingleLenderTxFactory, SingleRepaymentTxFactory}
import org.ergoplatform.appkit.{Address, ErgoContract, Parameters, SignedTransaction}

import scala.collection.JavaConverters.asScalaBufferConverter

case class FundRepaymentRunner(repaymentBoxId: String, funderAddress: String, explorer: LendBoxExplorer) {
  var paymentAddress: Address = Address.create("RShrjCJsjU5LPwWuzNexwJ42mdSy7dDERYMdtN3KbZSPeidJYNwzaCLT461hGtjrokYwoz8MQA4EMCM3FfwiJgdKTLEr3as5Bm3YSp8JVi2zaMZERxjBYPZAJACoMxweAVxXfgE9HDMYgiDUhE4qYyJTWwPNJXTuJBkfyBmXR4tRB3gVyCbUHrjHELrBm9nhcVfq2rg2VPTe5gxR574QqRKLSxzuVjjxsRvaREhvzstCTdyG1gBUwzWfze8Sy2esm9JQKYfuggrZAWQCB8aRt3H3gdqKHqYbK5JcrZe2kYcPhE9xnPr2aGDLNchpfV2kJ6nhL7G3kr9DCMyAfZMejvceRo")
  def proxyContract(client: Client, sendFunds: (ErgoContract, Long) => SignedTransaction, fundAmount: Long = 0): SignedTransaction = {
    val repaymentBox = explorer.getRepaymentBox(repaymentBoxId)
    val wrappedRepaymentBox = new SingleLenderRepaymentBox(repaymentBox)

    val lendProxyContractService = new LendProxyContractService(client)
    val paymentAddressContract = lendProxyContractService.getFundRepaymentBoxProxyContract(
      repaymentBoxId = repaymentBoxId,
      funderPk = funderAddress
    )

    val paymentAddressString = lendProxyContractService.getFundRepaymentBoxProxyContractString(
      repaymentBoxId = repaymentBoxId,
      funderPk = funderAddress
    )

    paymentAddress = Address.create(paymentAddressString)

    var totalAmountToFund = if (fundAmount == 0)
      wrappedRepaymentBox.getFullFundAmount
    else
      wrappedRepaymentBox.getFundAmount(fundAmount)

    val stringBuilder = new StringBuilder()
    stringBuilder.append(s"pk: ${walletAddress}\n")
    stringBuilder.append(s"boxId: ${repaymentBoxId}\n")
    stringBuilder.append(s"\n")
    stringBuilder.append(s"paymentAddress: ${paymentAddress}\n")
    stringBuilder.append(s"paymentValue: ${totalAmountToFund/Parameters.OneErg} Ergs\n")
    writeToFile("FundLendReq.txt", stringBuilder.toString())

    sendFunds(paymentAddressContract, totalAmountToFund)
  }

  def refundProxyPayment(client: Client, explorer: LendBoxExplorer): Unit = {
    val repaymentBox = explorer.getRepaymentBox(repaymentBoxId)
    val wrappedRepaymentBox = new SingleLenderRepaymentBox(repaymentBox)

    System.out.println(s"Getting boxes for ${paymentAddress.toString}")

    // get payment box
    val unspentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, wrappedRepaymentBox.getFullFundAmount).getBoxes.asScala

    System.out.println(s"${unspentPaymentBoxes.size} box found...")

    val refundTx = new RefundProxyContractTx(unspentPaymentBoxes, walletAddress)

    client.getClient.execute(ctx => {
      val signedTx = refundTx.runTx(ctx)

      var refundTxId = ctx.sendTransaction(signedTx)

      if (refundTxId == null) throw failedTxException(s"Refund failed for ${paymentAddress}")

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

  def handleProxyMerge(client: Client, explorer: LendBoxExplorer): Unit = {
    val repaymentBox = explorer.getRepaymentBox(repaymentBoxId)
    val wrappedRepaymentBox = new SingleLenderRepaymentBox(repaymentBox)

    System.out.println(s"Getting boxes for ${paymentAddress.toString}")

    val unspentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, wrappedRepaymentBox.getFullFundAmount).getBoxes.asScala

    System.out.println(s"${unspentPaymentBoxes.size} box found...")

    val singleAddressRegister = new SingleAddressRegister(walletAddress)
    val fundRepaymentTx = SingleRepaymentTxFactory.createLenderFundRepaymentTx(
      repaymentBox,
      unspentPaymentBoxes,
      singleAddressRegister)

    client.getClient.execute(ctx => {
      val signedTransaction = fundRepaymentTx.runTx(ctx)
      val fundTxId = ctx.sendTransaction(signedTransaction)

      System.out.println(signedTransaction.toJson(true))
      if (fundTxId == null) throw failedTxException(s"Fund Repayment failed for ${fundTxId}")
    })
  }

  def handleSuccess(client: Client, explorer: LendBoxExplorer): Unit = {
    val serviceBox = explorer.getServiceBox
    val repaymentBox = explorer.getRepaymentBox(repaymentBoxId)

    val fundSuccessTx = SingleRepaymentTxFactory.createSingleLenderRepaymentFundedTx(serviceBox, repaymentBox)

    client.getClient.execute(ctx => {
      val signedTransaction = fundSuccessTx.runTx(ctx)

      val fundSuccessTxId = ctx.sendTransaction(signedTransaction)

      System.out.println(signedTransaction.toJson(true))
      if (fundSuccessTxId == null) throw failedTxException(s"repayment fund success tx failed ${fundSuccessTxId}")

      System.out.println(s"Repayment Fund Success Tx ID: ${fundSuccessTxId}")
      System.out.println()
      System.out.println(signedTransaction)
      System.out.println()
      System.out.println(s"Repayment Box Id: ${signedTransaction.getOutputsToSpend.get(1).getId}")
    })
  }
}

object RepaymentRunner {
  val repaymentBoxId = ""
  val proxyPaymentAddress = ""
  val walletAddress = "9f83nJY4x9QkHmeek6PJMcTrf2xcaHAT3j5HD5sANXibXjMUixn"
  val repaymentBoxPaymentValue = 0

  def createFundRepaymentProxyContract(client: Client, explorer: LendBoxExplorer): ErgoContract = {
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
        throw e
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
      val unspentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, repaymentBoxPaymentValue).getBoxes.asScala

      System.out.println(s"${unspentPaymentBoxes.size} box found...")

      val refundTx = new RefundProxyContractTx(unspentPaymentBoxes, walletAddress)

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
