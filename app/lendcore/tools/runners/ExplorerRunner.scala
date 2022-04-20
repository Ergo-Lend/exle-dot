package lendcore.tools.runners

import features.lend.LendBoxExplorer
import lendcore.components.ergo.Client

import java.io.{File, PrintWriter}


/**
 *
 */
object ExplorerRunner {
  val walletAddress = "9f83nJY4x9QkHmeek6PJMcTrf2xcaHAT3j5HD5sANXibXjMUixn"
  def main(args: Array[String]): Unit = {

    val client = new Client
    client.setClient()
    val explorer = new LendBoxExplorer(client)

    // Goal: To go through the whole Lend transaction
    // 1. Create Lend Fund
    //    a. LendCreation Proxy Contract
    //    b. Run creation handle requests
    //
    // 2. Fund Lend Fund
    //    a. LendFund Proxy Contract
    //    b. Run fund handle requests
    //
    // 3. Lend Fund Success
    //    a. Run Success Transaction
    //
    // 4. Fund Repayment Fund
    //    a. RepaymentFund Proxy Contract
    //    b. Run fund Repayment handle requests
    //
    // 5. Repayment Success
    //    a. Run Repayment Success Transaction

      val runTx = "createLendFund"

      val signedTx: Unit = runTx match {
        case "createLendFund" => createLendFund(client, explorer)
        case "fundLendFund" => fundLendBox(client, explorer)
        case "fundRepayment" => repayment(client, explorer)
      }

//      val jsonVal = signedTx.toJson(true)
//      System.out.println(jsonVal)
//      jsonVal
  }

  def createLendFund(client: Client, explorer: LendBoxExplorer): Unit = {

    val tx = "handleInitiation"

    tx match {
      case "proxyContract" => {
        LendCreationRunner.createLendInitiationProxyContract(client)
      }
      case "handleInitiation" => {
        LendCreationRunner.handleLendInitiation(client, explorer)
      }
      case "handleRefund" => {
        LendCreationRunner.handleRefundProxy(client)
      }
      case _ => {
        LendCreationRunner.createLendInitiationProxyContract(client)
      }
    }
  }

  def fundLendBox(client: Client, explorer: LendBoxExplorer): Unit = {
    val tx = "proxyContract"

    tx match {
      case "proxyContract" => FundLendRunner.createFundLendProxyContract(client, explorer)
//      case "handleFund" => FundLendRunner.handleFundLend(client, explorer)
      case "handleRefundProxy" => FundLendRunner.handleRefundProxy(client)
      case "handleRefundLend" => FundLendRunner.refundLend(client, explorer)
      case "fundSuccess" => FundLendRunner.lendFundSuccess(client, explorer)
    }
  }

  def repayment(client: Client, explorer: LendBoxExplorer): Unit = {
    val tx = "proxyContract"

    tx match {
      case "proxyContract" => RepaymentRunner.createFundRepaymentProxyContract(client, explorer)
//      case "handleFund" => RepaymentRunner.handleFundRepayment(client, explorer)
      case "handleRefundProxy" => RepaymentRunner.handleRefundProxy(client)
      case "repaymentSuccess" => RepaymentRunner.repaymentSuccess(client, explorer)
    }
  }


  def writeToFile(fileName: String, fileContent: String): Unit = {
    val printWriter = new PrintWriter(new File(fileName))
    printWriter.write(fileContent)
    printWriter.close()
  }
}
