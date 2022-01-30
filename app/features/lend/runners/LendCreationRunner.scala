package features.lend.runners

import ergotools.client.Client
import errors.failedTxException
import features.lend.LendBoxExplorer
import features.lend.boxes.SingleLenderLendBox
import features.lend.boxes.registers.{BorrowerRegister, FundingInfoRegister, LendingProjectDetailsRegister}
import features.lend.contracts.proxyContracts.LendProxyContractService
import features.lend.runners.ExplorerRunner.{walletAddress, writeToFile}
import features.lend.txs.singleLender.{ProxyContractTx, SingleLenderTxFactory}
import org.ergoplatform.appkit.{Address, InputBox, Parameters}
import play.api.libs.json.JsResult.Exception

object LendCreationRunner {

  val lendBoxCreationPayment: Long = SingleLenderLendBox.getLendBoxInitiationPayment()
  val paymentAddressString: String = "S4zcopbihvfXvFPWQSKD2KiPi8TCsKqTRgfXxEYJZyxqw3BC81P3VDPPE7HT8tSQUtaK7WncHDuw1yCuPnJTSpgZCy8tKHsaFeLjCZfQPkEprhGguwTPHA6YXLehRXzNktz5Yee9P662T6GKdCkCvzqm2aADWhgNXjBNVxKJz6HFbZMfitZJBbVi4NCypELQpnMQk9iXVzB6miBMvbriVVsoiqrnkhwZXVNGrCRZpeqMdpo2ANLS7y88QXyStSQmDioVhFKGzmT6bhoDCmm8sXrwP3NDeoyQa2eCUcNLSh72Qaf2C1hU7dZievThqeBjLv23MrKq5KRnXMtXMZGn8bZXTiZ84MftipLC9FkQvRMAjmS9vmCpn94RrLrX8UqdyFEfmeTwAbnbcs6aLbPenFw2CuDNzZcxnzyvJ5q5DRxaKLXVB4RPefTBTUBLXt9EowBZk7EfekUcYPuakEete9gTs"

  val name: String = "Test Lend 2.0"
  val description: String = "A test lending box"
  val deadlineHeightRecorded: Long = 685163
  val repaymentHeightRecorded: Long = 695163

  val goal: Long = Parameters.OneErg / 100
  val interestRate: Long = 8

  def createLendInitiationProxyContract(client: Client): String = {
    try {

      val lendProxyContractService = new LendProxyContractService(client)
      val deadlineHeight = client.getHeight + 10000
      val interestRate = 8
      val repaymentHeightLength = client.getHeight + 20000

      val paymentAddress = lendProxyContractService.getLendCreateProxyContract(
        pk = walletAddress,
        deadlineHeight = deadlineHeight,
        goal = goal,
        interestRate = interestRate,
        repaymentHeightLength = repaymentHeightLength
      )

      val requiredPaymentInErgs: Float = lendBoxCreationPayment/Parameters.OneErg.toFloat

      val stringBuilder = new StringBuilder()
      stringBuilder.append(s"pk: ${walletAddress}\n")
      stringBuilder.append(s"projectName: ${name}\n")
      stringBuilder.append(s"description: ${description}\n")
      stringBuilder.append(s"deadlineHeight: ${deadlineHeight}\n")
      stringBuilder.append(s"goal: ${goal}\n")
      stringBuilder.append(s"interestRate: ${interestRate}\n")
      stringBuilder.append(s"repaymentHeightLength: ${repaymentHeightLength}\n")
      stringBuilder.append(s"\n")
      stringBuilder.append(s"paymentAddress: ${paymentAddress}\n")
      stringBuilder.append(s"paymentValue: ${requiredPaymentInErgs} Ergs\n")
      writeToFile("CreateLendReqInfo.txt", stringBuilder.toString())

      System.out.println(paymentAddress)
      System.out.println(s"deadlineHeight: ${deadlineHeight}")
      System.out.println(s"repaymentHeightLength: ${repaymentHeightLength}")
      System.out.println(s"Send value of: ${requiredPaymentInErgs} Ergs")

      paymentAddress
    } catch {
      case e: Exception => {
        System.out.println(e)
        return e.toString
      }
    }
  }

  def handleLendInitiation(client: Client, explorer: LendBoxExplorer): Unit = {
    client.getClient.execute(ctx => {
      val paymentAddress: Address = Address.create(paymentAddressString)

      System.out.println(s"Getting boxes for ${paymentAddressString}")

      // get payment box
      val unspentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, lendBoxCreationPayment).getBoxes

      System.out.println(s"${unspentPaymentBoxes.size()} box found...")

      val lendServiceBoxInputBox: InputBox = explorer.getServiceBox
      val fundingInfoRegister = new FundingInfoRegister(goal, deadlineHeightRecorded, interestRate, repaymentHeightRecorded)
      val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(name, description)
      val borrowerRegister = new BorrowerRegister(walletAddress)
      val lendInitiationTx = SingleLenderTxFactory.createLendInitiationTx(
        lendServiceBoxInputBox,
        unspentPaymentBoxes.get(0),
        fundingInfoRegister,
        lendingProjectDetailsRegister,
        borrowerRegister)

      val signedTx = lendInitiationTx.runTx(ctx)

      var createTxId = ctx.sendTransaction(signedTx)

      if (createTxId == null) throw failedTxException(s"Creation failed for ${paymentAddressString}")

      val stringBuilder = new StringBuilder()
      stringBuilder.append(s"Create Tx ID: ${createTxId} \n")
      stringBuilder.append(s"BoxId: ${signedTx.getOutputsToSpend.get(1).getId} \n")
      writeToFile("LendInitiation.txt", stringBuilder.toString())

      System.out.println(s"Create Tx ID: ${createTxId}")
      System.out.println()
      System.out.println(signedTx)
      System.out.println()
      System.out.println(s"Box Id: ${signedTx.getOutputsToSpend.get(1).getId}")
    })
  }

  def handleRefundProxy(client: Client): Unit = {
    client.getClient.execute(ctx => {
      val paymentAddress: Address = Address.create(paymentAddressString)

      System.out.println(s"Getting boxes for ${paymentAddressString}")

      // get payment box
      val unspentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, lendBoxCreationPayment).getBoxes

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
  }
}
