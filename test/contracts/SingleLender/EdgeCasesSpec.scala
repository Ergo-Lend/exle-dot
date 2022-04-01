package contracts.SingleLender

import config.Configs
import contracts.{client, dummyAddress, dummyProver, dummyTxId, ergoClient}
import ergotools.LendServiceTokens
import features.lend.boxes.{FundsToAddressBox, LendServiceBox, SingleLenderLendBox}
import features.lend.boxes.registers.{BorrowerRegister, FundingInfoRegister, LendingProjectDetailsRegister, SingleLenderRegister}
import features.lend.contracts.proxyContracts.LendProxyContractService
import org.ergoplatform.appkit.{ErgoContract, Parameters, SignedTransaction, UnsignedTransaction}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sigmastate.lang.exceptions.InterpreterException

import scala.collection.JavaConverters.seqAsJavaListConverter

class EdgeCasesSpec extends AnyWordSpec with Matchers {
  val serviceBox: LendServiceBox = buildGenesisServiceBox()
  val goal: Long = 1e9.toLong
  val interestRate = 100
  val repaymentHeightLength = 100
  val deadlineHeightLength = 100
  val loanName = "Test Loan"
  val loanDescription = "Test Loan Description"

  client.setClient()
  val lendProxyContractService = new LendProxyContractService(client)

  "Edge cases" when {
    "interest rates is at 0%" should {
      "passes on initiation" in {
        val tx = instantiateLendBox(interestRate = 0)
        dummyProver.sign(tx)
      }
    }

    "interest rates set to 120%" should {
      "passes on initiation" in {
        val tx = instantiateLendBox(interestRate = 1200)
        dummyProver.sign(tx)
      }
    }

    "interest rate is -ve" should {
      "fail" in {
        val tx = instantiateLendBox(interestRate = -10)
        assertThrows[InterpreterException] {
          dummyProver.sign(tx)
        }
      }
    }

    "funding height is -ve" should {
      "fail" in {
        val tx = instantiateLendBox(deadlineHeight = -100)
        assertThrows[InterpreterException] {
          dummyProver.sign(tx)
        }
      }
    }

    "repayment height is -ve" should {
      "fail" in {
        val tx = instantiateLendBox(repaymentHeight = -100)
        assertThrows[InterpreterException] {
          dummyProver.sign(tx)
        }
      }
    }

    def instantiateLendBox(interestRate: Long = interestRate,
                           deadlineHeight: Long = deadlineHeightLength,
                           repaymentHeight: Long = repaymentHeightLength,
                           fundingGoal: Long = goal,
                           borrowerAddress: String = dummyAddress.toString): UnsignedTransaction = {
      ergoClient.execute {
        ctx => {
          val txB = ctx.newTxBuilder()

          // Input Boxes
          val inputServiceBox = serviceBox
            .getOutputServiceBox(ctx, txB)
            .convertToInputWith(dummyTxId, 0)

          val lendCreationProxyContract: ErgoContract =
            lendProxyContractService.getLendCreateProxyContract(
              pk = borrowerAddress,
              deadlineHeight = ctx.getHeight + deadlineHeight,
              goal = fundingGoal,
              interestRate = interestRate,
              repaymentHeightLength = repaymentHeight
            )

          val inputProxyContract = txB.outBoxBuilder()
            .contract(lendCreationProxyContract)
            .value(Parameters.MinFee * 3 + Configs.serviceFee)
            .build()
            .convertToInputWith(dummyTxId, 0)

          // Output Boxes
          val outputServiceBox = serviceBox.createLend().getOutputServiceBox(ctx, txB)
          val fundingInfoRegister = new FundingInfoRegister(
            fundingGoal = fundingGoal,
            deadlineHeight = ctx.getHeight + deadlineHeight,
            interestRatePercent = interestRate,
            repaymentHeightLength = repaymentHeight
          )
          val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(
            projectName = loanName,
            description = loanDescription,
          )
          val borrowerRegister = new BorrowerRegister(dummyAddress.toString)
          val lendBox = new SingleLenderLendBox(
            value = Parameters.MinFee,
            fundingInfoRegister = fundingInfoRegister,
            lendingProjectDetailsRegister = lendingProjectDetailsRegister,
            borrowerRegister = borrowerRegister,
            singleLenderRegister = SingleLenderRegister.emptyRegister
          ).getOutputBox(ctx, txB)

          val outputOwnerFeeBox = new FundsToAddressBox(
            value = Configs.serviceFee,
            address = Configs.serviceOwner).getOutputBox(ctx, txB)

          val tx = txB.boxesToSpend(Seq(inputServiceBox, inputProxyContract).asJava)
            .fee(Parameters.MinFee)
            .outputs(outputServiceBox, lendBox, outputOwnerFeeBox)
            .sendChangeTo(dummyAddress.getErgoAddress)
            .build()

          tx
        }
      }
    }
  }
}
