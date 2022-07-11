package commons.registers

import commons.boxes.registers.RegisterTypes.{LongRegister}
import org.ergoplatform.appkit.ErgoValue
import special.collection.Coll

/**
  * Register: RepaymentDetails
  *
  * Stores the details for repayment when a lendbox is successfully funded and a repayment box is created
  *
  * Repayment Amount Calculation
  * Financial Equation: (Total + (Total * InterestRate * Time
  * Equation: (fundingGoal + (fundingGoal * interestRate * (1 + (height - fundingDeadlineHeight/repaymentHeightGoal))
  *
  * @param fundedHeight Height of blockchain when lendbox is funded and spent (repayment box gets created)
  * @param repaymentAmount the amount of currency/token to be repaid
  * @param totalInterestAmount total Interest
  * @param repaymentHeightGoal the optimal time for repayment to be paid (can be used for Credit system)
  */
final case class RepaymentDetailsRegister(
  fundedHeight: Long,
  repaymentAmount: Long,
  totalInterestAmount: Long,
  repaymentHeightGoal: Long
) extends LongRegister
    with RepaymentRegister {

  def toRegister: ErgoValue[Coll[Long]] = {
    val register: Array[Long] = new Array[Long](4)

    register(0) = fundedHeight
    register(1) = repaymentAmount
    register(2) = totalInterestAmount
    register(3) = repaymentHeightGoal

    ergoValueOf(register)
  }

  def this(values: Array[Long]) = this(
    fundedHeight = values(0),
    repaymentAmount = values(1),
    totalInterestAmount = values(2),
    repaymentHeightGoal = values(3)
  )
}

object RepaymentDetailsRegister {

  def apply(
    fundedHeight: Long,
    fundingInfoRegister: FundingInfoRegister
  ): RepaymentDetailsRegister = {
    val fundingGoal = fundingInfoRegister.fundingGoal
    val interestRate = fundingInfoRegister.interestRatePercent
    val repaymentHeightLength = fundingInfoRegister.repaymentHeightLength

    val repaymentHeightGoal = fundedHeight + repaymentHeightLength
    val totalInterestAmount = (fundingGoal * interestRate) / 1000
    val repaymentAmount = fundingGoal + totalInterestAmount

    new RepaymentDetailsRegister(
      fundedHeight,
      repaymentAmount,
      totalInterestAmount,
      repaymentHeightGoal
    )
  }

  def calculateInterestRate(fundingGoal: Long, interestRate: Long): Long = {
    val totalInterestAmount = (fundingGoal * interestRate) / 1000

    totalInterestAmount
  }
}

trait RepaymentRegister
