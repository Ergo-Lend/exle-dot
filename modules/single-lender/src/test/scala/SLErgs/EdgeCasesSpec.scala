package SLErgs

import SLErgs.boxes.{FundsToAddressBox, SLELendBox, SLEServiceBox}
import commons.configs.{Configs, ServiceConfig}
import SLErgs.contracts._
import commons.registers.{
  BorrowerRegister,
  FundingInfoRegister,
  LendingProjectDetailsRegister,
  SingleLenderRegister
}
import org.ergoplatform.appkit.{ErgoContract, Parameters, UnsignedTransaction}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sigmastate.lang.exceptions.InterpreterException

import scala.collection.JavaConverters.seqAsJavaListConverter

class EdgeCasesSpec extends AnyWordSpec with Matchers {
  val serviceBox: SLEServiceBox = buildGenesisServiceBox()
  val goal: Long = 1e9.toLong
  val interestRate: Long = 100L
  val repaymentHeightLength: Long = 100L
  val deadlineHeightLength: Long = 100L
  val loanName: String = "Test Loan"
  val loanDescription: String = "Test Loan Description"

  val service: ServiceConfig.type = ServiceConfig

  client.setClient()
  val lendProxyContractService = new LendProxyContractService(client)

  "Edge cases" when {
    "interest rates is at 0%" should {
      "passes on initiation" in {
        val tx = instantiateLendBox(interestRate = 0)
        dummyProver.sign(tx)
      }

      "passes on default consumption" in {
        client.getClient.execute { ctx =>
          val txB = ctx.newTxBuilder()

          val wrappedServiceBox = buildGenesisServiceBox()
          val wrappedRepaymentBox = createRawWrappedRepaymentBox(
            interestRate = 0,
            fundedRepaymentHeight = (ctx.getHeight - 100).toLong
          ).fundedBox()

          val inputServiceBox = wrappedServiceBox
            .getOutputServiceBox(ctx, txB)
            .convertToInputWith(dummyTxId, 0)
          val inputRepaymentBox = wrappedRepaymentBox
            .getOutputBox(ctx, txB)
            .convertToInputWith(dummyTxId, 0)

          // Output Boxes
          val outputBoxes = wrappedServiceBox
            .consumeRepaymentBox(repaymentBox = wrappedRepaymentBox, ctx, txB)
            .asJava

          val totalInputVal =
            inputServiceBox.getValue + inputRepaymentBox.getValue
          val totalOutputVal =
            outputBoxes.get(0).getValue + outputBoxes.get(1).getValue
          val outputServiceBox =
            outputBoxes.get(0).convertToInputWith(dummyTxId, 0)
          val outputLenderBox =
            outputBoxes.get(1).convertToInputWith(dummyTxId, 0)

          assert(
            totalInputVal - totalOutputVal == Parameters.MinFee,
            "Input, output value in-balance"
          )
          assert(
            inputServiceBox.getTokens
              .get(2)
              .getValue == outputServiceBox.getTokens.get(2).getValue - 1
          )
          assert(
            outputLenderBox.getValue >= wrappedRepaymentBox.fundingInfoRegister.fundingGoal
          )
          assert(
            Configs.addressEncoder
              .fromProposition(outputLenderBox.getErgoTree)
              .get == dummyAddress.getErgoAddress
          )
          assert(
            inputRepaymentBox.getValue >= wrappedRepaymentBox.repaymentDetailsRegister.repaymentAmount
          )

          val tx = txB
            .boxesToSpend(Seq(inputServiceBox, inputRepaymentBox).asJava)
            .fee(Parameters.MinFee)
            .outputs(outputBoxes.get(0), outputBoxes.get(1))
            .sendChangeTo(dummyAddress.getErgoAddress)
            .build()

          dummyProver.sign(tx)
        }
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

    def instantiateLendBox(
      interestRate: Long = interestRate,
      deadlineHeight: Long = deadlineHeightLength,
      repaymentHeight: Long = repaymentHeightLength,
      fundingGoal: Long = goal,
      borrowerAddress: String = dummyAddress.toString
    ): UnsignedTransaction =
      client.getClient.execute { ctx =>
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

        val inputProxyContract = txB
          .outBoxBuilder()
          .contract(lendCreationProxyContract)
          .value(Parameters.MinFee * 3 + service.serviceFee)
          .build()
          .convertToInputWith(dummyTxId, 0)

        // Output Boxes
        val outputServiceBox =
          serviceBox.createLend().getOutputServiceBox(ctx, txB)
        val fundingInfoRegister = FundingInfoRegister(
          fundingGoal = fundingGoal,
          deadlineHeight = ctx.getHeight + deadlineHeight,
          interestRatePercent = interestRate,
          repaymentHeightLength = repaymentHeight,
          creationHeight = client.getHeight
        )
        val lendingProjectDetailsRegister = LendingProjectDetailsRegister(
          projectName = loanName,
          description = loanDescription
        )
        val borrowerRegister = new BorrowerRegister(dummyAddress.toString)
        val lendBox = new SLELendBox(
          value = Parameters.MinFee,
          fundingInfoRegister = fundingInfoRegister,
          lendingProjectDetailsRegister = lendingProjectDetailsRegister,
          borrowerRegister = borrowerRegister,
          singleLenderRegister = SingleLenderRegister.emptyRegister
        ).getOutputBox(ctx, txB)

        val outputOwnerFeeBox = FundsToAddressBox(
          value = service.serviceFee,
          address = service.serviceOwner
        ).getOutputBox(txB)

        val tx = txB
          .boxesToSpend(Seq(inputServiceBox, inputProxyContract).asJava)
          .fee(Parameters.MinFee)
          .outputs(outputServiceBox, lendBox, outputOwnerFeeBox)
          .sendChangeTo(dummyAddress.getErgoAddress)
          .build()

        tx
      }
  }
}
