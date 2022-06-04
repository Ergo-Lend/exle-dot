{
    // ===== Contract Info ===== //
    // Name             : SLE [Single Lender Ergs] Fund Repayment Box Proxy Contract
    // Description      : A contract to ensure that the funding goes to the right
    //                    repayment box and if there is any overfunded amount, this script
    //                    ensures that the overfunded payment goes back to the lender.
    // Type             : Proxy Contract
    // Author           : Kii
    // Last Modified    : May 8th 2022
    // Version          : v 1.0
    // Status           : Completed

    // ===== Contract Hard-Coded Constants ===== //
    // val _BoxIdToFund:                    Coll[Byte]
    // val _FunderPk:                       Coll[Byte]
    // val _MinFee:                         Long
    // val _SLERepaymentTokenId:            Coll[Byte]

    // ===== Contract Conditions ===== //
    // 1. Fund Repayment
    // 2. Refund

    if (INPUTS.size == 1) {
        // ** REFUND **
        //
        // else refund the amount repaid (this refund will not be transacted alongside an input of repayment box)
        // it has to be processed purely as a refund. Therefore it will be used as the only input
        val returnRepayment = {
            allOf(Coll(
                OUTPUTS(0).value <= (INPUTS(0).value - _MinFee),
                OUTPUTS(0).propositionBytes == _FunderPk, // user must receive the transaction back to his account
            ))
        }

        sigmaProp(returnRepayment)
    } else {

        // ** Variable Declaration **
        val inputRepaymentBox = INPUTS(0)
        val outputRepaymentBox = OUTPUTS(0)
        val repaymentBoxRepaymentDetails = inputRepaymentBox.R8[Coll[Long]]

        val repaymentGoal = repaymentBoxRepaymentDetails.get(1)
        val repaymentBoxId = inputRepaymentBox.id

        val amountToRepay = SELF.value - _MinFee
        val amountRepaid = inputRepaymentBox.value

        val amountRepaidOutput = amountToRepay + amountRepaid


        // ** Fund **
        val boxIdCheck = _BoxIdToFund == repaymentBoxId
        val repaymentGoalReached = repaymentGoal <= amountRepaid
        val fundable = boxIdCheck && !repaymentGoalReached
        if (fundable) {

            // we check the id and if the value is correct.
            val repaymentCheck = {
                allOf(Coll(
                    fundable,
                    inputRepaymentBox.tokens(0)._1 == _SLERepaymentTokenId
                ))
            }

            // Check if it will overfund, if it does, return overflow
            // funds to funder
            val fullFundedRepaymentBoxValue = repaymentGoal + _MinFee
            val overfunded = amountRepaidOutput > (fullFundedRepaymentBoxValue)
            if (overfunded) {

                // 1. RepaymentBoxFunded Appropriately
                // 2. Fund back to lender with right funds
                val repaymentGoalBox = outputRepaymentBox.value == fullFundedRepaymentBoxValue
                val fundsBackToFunderBox = OUTPUTS(1)
                val fundsBackToFunder = fundsBackToFunderBox.propositionBytes == _FunderPk

                val overfundedSigma = {
                    allOf(Coll(
                        repaymentGoalBox,
                        fundsBackToFunderBox.value == (amountRepaidOutput - fullFundedRepaymentBoxValue),
                        fundsBackToFunder
                    ))
                }

                sigmaProp(repaymentCheck && overfundedSigma)
            } else {
                val valueTransferred = outputRepaymentBox.value == amountRepaidOutput

                sigmaProp(repaymentCheck && valueTransferred)
            }

        } else {
            sigmaProp(false)
        }
    }
}