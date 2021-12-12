package features.lend.boxes.registers

import boxes.registers.RegisterTypes.CollByteRegister
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
 * @param repaymentHeightGoal the optimal time for repayment to be paid (can be used for Credit system)
 */
class RepaymentDetailsRegister(fundedHeight: Long,
                               repaymentAmount: Long,
                               repaymentHeightGoal: Long) extends CollByteRegister with RepaymentRegister {

  def apply(fundedHeight: Long, fundingInfoRegister: FundingInfoRegister): RepaymentDetailsRegister = {
    val fundingGoal = fundingInfoRegister.fundingGoal
    val interestRate = fundingInfoRegister.interestRatePercent
    val repaymentHeightLength = fundingInfoRegister.repaymentHeightLength

    val repaymentHeightGoal = fundedHeight + repaymentHeightLength
    val repaymentAmount = fundingGoal + (fundingGoal * interestRate/100)

    new RepaymentDetailsRegister(fundedHeight, repaymentAmount, repaymentHeightGoal)
  }

  def toRegister: ErgoValue[Coll[Long]] = {
    val register: Array[Long] = new Array[Long](3)

    register(0) = fundedHeight
    register(1) = repaymentAmount
    register(2) = repaymentHeightGoal

    ergoValueOf(register)
  }
}

trait RepaymentRegister