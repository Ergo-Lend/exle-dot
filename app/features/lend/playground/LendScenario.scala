package features.lend.playground

import org.ergoplatform.compiler.ErgoScalaCompiler._
import org.ergoplatform.playgroundenv.utils.ErgoScriptCompiler
import org.ergoplatform.playground._
import org.ergoplatform.playgroundenv.models.TokenAmount
import org.ergoplatform.DataInput
import org.ergoplatform.appkit.Parameters

object LendScenario {
  def main(args: Array[String]): Unit = {
    val blockchainSimulation = newBlockChainSimulationScenario("ErgoLend")

    val serviceNFT = blockchainSimulation.newToken("serviceNFT")
    val serviceNFTAmount = 1L

    val lendToken = blockchainSimulation.newToken("LendToken")
    val lendTokenAmount = 1000000000L

    val repaymentToken = blockchainSimulation.newToken("RepaymentToken")
    val repaymentTokenAmount = 1000000000L

    val serviceOwner = blockchainSimulation.newParty("ErgoLendServiceWallet")
    val borrower = blockchainSimulation.newParty("Borrower")
    val lender = blockchainSimulation.newParty("Lender")

    val minErg = Parameters.MinFee
    val nanoergsInErg = Parameters.OneErg

    val borrowerBalance = 10 * nanoergsInErg
    borrower.generateUnspentBoxes(
      toSpend = borrowerBalance
    )

    val lenderBalance = 1000 * nanoergsInErg
    lender.generateUnspentBoxes(
      toSpend = lenderBalance
    )

    borrower.printUnspentAssets()
    println("------------------")
    lender.printUnspentAssets()
    println("-------------------")

    val serviceOwnerFunds = nanoergsInErg
    serviceOwner.generateUnspentBoxes(
      toSpend = serviceOwnerFunds,
      tokensToSpend = List(
        serviceNFT -> serviceNFTAmount,
        lendToken -> lendTokenAmount,
        repaymentToken -> repaymentTokenAmount
      )
    )
    serviceOwner.printUnspentAssets()
    println("-------------")


  }
}
