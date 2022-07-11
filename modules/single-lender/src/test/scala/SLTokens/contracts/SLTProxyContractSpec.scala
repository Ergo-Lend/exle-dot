package SLTokens.contracts

import SLErgs.boxes.FundsToAddressBox
import common.ErgoTestBase
import org.ergoplatform.appkit.{
  ErgoContract,
  InputBox,
  OutBox,
  UnsignedTransactionBuilder
}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sigmastate.lang.exceptions.InterpreterException

import scala.collection.JavaConverters.seqAsJavaListConverter

class SLTProxyContractSpec extends ErgoTestBase {
  val SLTProxyContractService = new SLTProxyContractService(client)

  "SLT CreateLendBox ProxyContract" when {
    val sltLendCreateProxyContract: ErgoContract =
      SLTProxyContractService
        .getSLTLendCreateProxyContract(
          borrowerPk = dummyAddress.toString,
          loanToken = loanToken,
          deadlineHeight = deadlineHeightLength,
          goal = goal,
          interestRate = interestRate,
          repaymentHeightLength = repaymentHeightLength
        )

    val createSLTLendPaymentBox: InputBox = createPaymentBox(
      sltLendCreateProxyContract,
      serviceFee
    )

    "Creating" should {
      "Not be hacked by others into their account" in {
        client.getClient.execute { ctx =>
          // TxB create for other address
          val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
          val outputHackerBox: OutBox = new FundsToAddressBox(
            value = serviceFee - minFee,
            address = hackerAddress
          ).getOutputBox(ctx, txB)

          val tx = txB
            .boxesToSpend(Seq(createSLTLendPaymentBox).asJava)
            .fee(minFee)
            .outputs(outputHackerBox)
            .sendChangeTo(dummyAddress.getErgoAddress)
            .build()

          assertThrows[InterpreterException] {
            dummyProver.sign(tx)
          }
        }
      }

      "Refund back to borrower" in {
        client.getClient.execute { ctx =>
          val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
          val outputBorrowerBox: OutBox = new FundsToAddressBox(
            value = serviceFee - minFee,
            address = dummyAddress
          ).getOutputBox(ctx, txB)

          val tx = txB
            .boxesToSpend(Seq(createSLTLendPaymentBox).asJava)
            .fee(minFee)
            .outputs(outputBorrowerBox)
            .sendChangeTo(dummyAddress.getErgoAddress)
            .build()

          dummyProver.sign(tx)
        }
      }

      "Ensure the box created details are correct" in {}

    }
  }

  "SLT FundLendBox ProxyContract" when {}

  "SLT FundRepaymentBox ProxyContract" when {}
}
