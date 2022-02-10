package features.lend.runners

import ergotools.client.Client
import errors.failedTxException
import features.lend.LendBoxExplorer
import features.lend.boxes.SingleLenderLendBox
import features.lend.boxes.registers.{BorrowerRegister, FundingInfoRegister, LendingProjectDetailsRegister}
import features.lend.contracts.proxyContracts.LendProxyContractService
import features.lend.runners.ExplorerRunner.{walletAddress, writeToFile}
import features.lend.runners.LendCreationRunner.{description, goal, name}
import features.lend.txs.singleLender.{ProxyContractTx, SingleLenderTxFactory}
import org.ergoplatform.appkit.{Address, ErgoContract, InputBox, Parameters, SignedTransaction}
import play.api.libs.json.JsResult.Exception

object Implicits {
  implicit class CaseClassToString(c: AnyRef) {
    def toStringWithFields: String = {
      val fields = (Map[String, Any]() /: c.getClass.getDeclaredFields) { (a, f) =>
        f.setAccessible(true)
        a + (f.getName -> f.get(c))
      }

      s"${c.getClass.getName}(${fields.mkString(", ")})"
    }
  }
}

case class LendInitiationDetails(val name: String = "Test Lend 2.0",
                                 val description: String = "A test Lending box",
                                 val goal: Long = Parameters.OneErg / 100,
                                 val interestRate: Long = 8,
                                 val deadlineHeight: Long,
                                 val repaymentHeight: Long,
                                 val walletAddress: String) {
  var lendBoxCreationPayment: Long = SingleLenderLendBox.getLendBoxInitiationPayment()
}

case class LendInitiationRunner(lendInitiationDetails: LendInitiationDetails) {
  var paymentAddress: Address = Address.create("S4zcopbihvfXvFPWR77PvzxXQSZLC2vtF7AVv1sGprtFD59cedcEq6h9MSgdsuMJuwfCL6M8UnRY2yL4CRAuD3cxXPjkgtbckYWe8V7QXVNSpCx5oQ8ZfK2Kspqu2L6tj6hLssXV2L6BBRb6mkPDgt5F77AUrayukeQfiCdfkxFVU1BCRHf3e8NBSDsDECehRjgt1xhrmxQAmipY6VE4AUecvGF8XKMgidGbXqsz8FGf7a7rQGx7tgSyJqD55Ao1D6puW5tki3iPg4ZbBV6eT59Xzcasmwo3oBEWHq8jQRTKBDEdH5jaUNVLDb2APppKc9Q2oywWJajenieqZy8ghovRk6oAZ9E4rFLDqDPzJLaNTcDS6KfudGVKQNWSh21gdESeFxYKn5PDRaJh2iLCd4QeBTjhwSLd4yU8FhAGPSPAySR1HVWcKhxGN1KhetgZg9iZabx1QR5HLERHTEunn7W1j")
  def proxyContract(client: Client, sendFunds: (ErgoContract, Long) => SignedTransaction): SignedTransaction = {
    val lendProxyContractService = new LendProxyContractService(client)
    val paymentAddressContract = lendProxyContractService.getLendCreateProxyContract(
      pk = lendInitiationDetails.walletAddress,
      deadlineHeight = lendInitiationDetails.deadlineHeight,
      goal = lendInitiationDetails.goal,
      interestRate = lendInitiationDetails.interestRate,
      repaymentHeightLength =  lendInitiationDetails.repaymentHeight
    )
    val paymentAddressString = lendProxyContractService.encodeAddress(paymentAddressContract)

    paymentAddress = Address.create(paymentAddressString)

    // Send Funds
    val stringBuilder = new StringBuilder()
    stringBuilder.append(s"pk: ${lendInitiationDetails.walletAddress}\n")
    stringBuilder.append(s"projectName: ${lendInitiationDetails.name}\n")
    stringBuilder.append(s"description: ${lendInitiationDetails.description}\n")
    stringBuilder.append(s"deadlineHeight: ${lendInitiationDetails.deadlineHeight}\n")
    stringBuilder.append(s"goal: ${lendInitiationDetails.goal}\n")
    stringBuilder.append(s"interestRate: ${lendInitiationDetails.interestRate}\n")
    stringBuilder.append(s"repaymentHeightLength: ${lendInitiationDetails.repaymentHeight}\n")
    stringBuilder.append(s"\n")
    stringBuilder.append(s"paymentAddress: ${paymentAddress}\n")
    stringBuilder.append(s"paymentValue: ${lendInitiationDetails.lendBoxCreationPayment/Parameters.OneErg} Ergs\n")
    writeToFile("CreateLendReqInfo.txt", stringBuilder.toString())

    sendFunds(paymentAddressContract, lendInitiationDetails.lendBoxCreationPayment)
  }

  def handleProxyMerge(client: Client, explorer: LendBoxExplorer): Unit = {
    System.out.println(s"Getting boxes for ${paymentAddress.toString}")

    // get payment box
    val unspentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, lendInitiationDetails.lendBoxCreationPayment).getBoxes

    System.out.println(s"${unspentPaymentBoxes.size()} box found...")

    val lendServiceBoxInputBox: InputBox = explorer.getServiceBox
    val fundingInfoRegister = new FundingInfoRegister(
      lendInitiationDetails.goal,
      lendInitiationDetails.deadlineHeight,
      lendInitiationDetails.interestRate,
      lendInitiationDetails.repaymentHeight)
    val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(lendInitiationDetails.name, lendInitiationDetails.description)
    val borrowerRegister = new BorrowerRegister(lendInitiationDetails.walletAddress)
    val lendInitiationTx = SingleLenderTxFactory.createLendInitiationTx(
      lendServiceBoxInputBox,
      unspentPaymentBoxes.get(0),
      fundingInfoRegister,
      lendingProjectDetailsRegister,
      borrowerRegister)

    client.getClient.execute(ctx => {
      val signedTx = lendInitiationTx.runTx(ctx)

      var createTxId = ctx.sendTransaction(signedTx)

      System.out.println(signedTx.toJson(true))

      if (createTxId == null) throw failedTxException(s"Creation failed for ${paymentAddress.toString}")
    })
  }
}

object LendCreationRunner {

  val lendBoxCreationPayment: Long = SingleLenderLendBox.getLendBoxInitiationPayment()
  val paymentAddressString: String = "S4zcopbihvfXvFPWQHBz5jxeRVosPDtPtP6W7xptmX2C9u4QxkdKTv2Uv9NrKHQeeevBBrDPSi2GQMhgxALRc2hy98CJxS8MrniJPrn3BnvnJhQg2pNQWYwfCVewUzFViGyBJqsZwS66we6PYznEdfLAAoFv3cfUSTgyJDVH1MPgNkFuP9sKVsNp7oy4aXkK28CUFJ9kcoWz6tgLTT8QbiZt43m3ZEm3e3fjQoAxhYGeLq61516JwRq2eh9ZHwAA3o84qzrp7n1CUTXHsJe4ugYA2KbZfSHyLKspxo7uu6nTEfyzJ2WiZRwEem31rtPgNqb5bYvvvVtQZvTDDxxVpUe71yQyatX7njAr6Ay5zGUNuQncJKtaC4fBEkiXxnD8x9buManiZyzG8uBS7ZnF8n4hNmViqxmQwfX7A4mSQvUsKq6q1THyB2c1qTjYmJbhvHaqt3NijTzS4nbP8RdRRYyxn"

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

      val paymentAddress = lendProxyContractService.getLendCreateProxyContractString(
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

