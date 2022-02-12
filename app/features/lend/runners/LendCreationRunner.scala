package features.lend.runners

import ergotools.client.Client
import errors.failedTxException
import features.lend.LendBoxExplorer
import features.lend.boxes.SingleLenderLendBox
import features.lend.boxes.registers.{BorrowerRegister, FundingInfoRegister, LendingProjectDetailsRegister}
import features.lend.contracts.proxyContracts.LendProxyContractService
import features.lend.runners.ExplorerRunner.{walletAddress, writeToFile}
import features.lend.txs.singleLender.{RefundProxyContractTx, SingleLenderTxFactory}
import org.ergoplatform.appkit.{Address, ErgoContract, InputBox, Parameters, SignedTransaction}
import play.api.libs.json.JsResult.Exception

import scala.collection.JavaConverters.asScalaBufferConverter

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
  var lendBoxCreationPayment: Long = SingleLenderLendBox.getLendBoxInitiationPayment
}

case class LendInitiationRunner(lendInitiationDetails: LendInitiationDetails) {
  var paymentAddress: Address = Address.create("6gRPAFQj5HgzknQE1Ufc6teW6K2SC7L1HLXFFjLYgPdXrrstaSy1sKWVB51NVkabsWbwDwEArLonzzd35rRuA5DgKDQHF2GYfQuUtzaArxNAvk34MUtyjSvnU6tzSPtjALErPhdvmwJXCfMKtDZtUfMyPtFbQ3Yxp1JkdmmK4jTCK8SFwaca5Qy86HzYqshZYjBx9twuVQdVhm6Asg1AVhG5fSMQ5TMZZCCVHTiQVukRc2HLFXgGd9CfhCXBNcy4eH4JuRkh8AGS8hEsLReUTenwtjE3iP9uj1xxbN42PbWVqkB5jgGxBkYu8JV65xTGJ7mizwjNTn95WtXUcdgUdqw3EDn5vnEbeVwxNNhKDECTRbwCj6W3G4sSzhpQw3pq4gmUVFKVFH6ANa4MbAfzyX1BWH9xt4T48dLPdS1ioXovxj3VpGshzr3yZEk7N7y3wvDSxGM8bmGGg9FKmpshpkmF")
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
    val unspentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, lendInitiationDetails.lendBoxCreationPayment).getBoxes.asScala

    System.out.println(s"${unspentPaymentBoxes.size} box found...")

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
      unspentPaymentBoxes,
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

  val lendBoxCreationPayment: Long = SingleLenderLendBox.getLendBoxInitiationPayment
  val paymentAddressString: String = ""

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
      val unspentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, lendBoxCreationPayment).getBoxes.asScala

      System.out.println(s"${unspentPaymentBoxes.size} box found...")

      val lendServiceBoxInputBox: InputBox = explorer.getServiceBox
      val fundingInfoRegister = new FundingInfoRegister(goal, deadlineHeightRecorded, interestRate, repaymentHeightRecorded)
      val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(name, description)
      val borrowerRegister = new BorrowerRegister(walletAddress)
      val lendInitiationTx = SingleLenderTxFactory.createLendInitiationTx(
        lendServiceBoxInputBox,
        unspentPaymentBoxes,
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
      val unspentPaymentBoxes = client.getCoveringBoxesFor(paymentAddress, lendBoxCreationPayment).getBoxes.asScala

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
  }
}

