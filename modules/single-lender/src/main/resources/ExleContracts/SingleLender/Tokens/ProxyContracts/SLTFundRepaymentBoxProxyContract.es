{
    // ===== Contract Info ===== //
    // Name             : SLT [Single Lender Tokens] Fund Repayment Box Proxy Contract
    // Description      : A contract to ensure that the funding goes to the right
    //                    repayment box and if there is any overfunded amount, this script
    //                    ensures that the overfunded payment goes back to the lender.
    // Type             : Proxy Contract
    // Author           : Kii
    // Last Modified    : May 28th 2022
    // Version          : v 1.0
    // Status           : 1st Draft Completed

    // ===== Contract Hard-Coded Constants ===== //
    // val _BoxIdToFund:                    Coll[Byte]
    // val _FunderPk:                       Coll[Byte]
    // val _MinFee:                         Long
    // val _SLTRepaymentTokenId:            Coll[Byte]

    // ===== Contract Conditions ===== //
    // 1. Fund Repayment
    // 2. Refund

    // ===== Refund ===== //
    // Description: Checks to see if the output box is of
    //              lender, and then returns the correct
    //              amount back to them.
    // Input Boxes      -> (0: PaymentBox)
    // Output Boxes     -> (0: RefundToFunderBox)
    val isRefund: Boolean = INPUTS.size == 1

    if (isRefund) {
        // @todo kelim ensure that boxes return value is correct
        val tokenValueRefunded: Boolean = {
            allOf(Coll(
                OUTPUTS(0).tokens(0)._1 == INPUTS(0).tokens(0)._1,
                OUTPUTS(0).tokens(0)._2 == INPUTS(0).tokens(0)._2
            ))
        }
        val refundToFunder: Boolean     = OUTPUTS(0).propositionBytes == _FunderPk

        sigmaProp(tokenValueRefunded && refundToFunder)
    } else {
        // ===== Variable Declaration ===== //
        val inputSltRepaymentBox: Box   = INPUTS(0)
        val fundRepaymentProxy: Box     = SELF
        val outputSltRepaymentBox: Box  = OUTPUTS(0)

        val repaymentDetails: Coll[Long]= inputSltRepaymentBox.R9[Coll[Long]]
        val repaymentGoal: Long         = repaymentDetails.get(1)
        val repaymentBoxId: Coll[Byte]  = inputSltRepaymentBox.id

        val amountRepaid: Long          = inputSltRepaymentBox.tokens(1)._2
        val allFundsOutput: Long        = amountRepaid + SELF.tokens(0)._2

        // ===== Fund ===== //
        val repaymentGoalReached: Boolean   = repaymentGoal <= amountRepaid
        val boxIdCheck: Boolean             = _BoxIdToFund == repaymentBoxId
        val repaymentCheck: Boolean         = inputSltRepaymentBox.tokens(0)._1 == _SLTRepaymentTokenId
        val fundable: Boolean               = boxIdCheck && !repaymentGoalReached && repaymentCheck

        if (fundable) {

            // ===== OverFunded ===== //
            // Input Boxes      -> 0: InputSltRepaymentBox, 1: SELF | PaymentBox
            // Output Boxes     -> 0: OutputSltRepaymentBox, 1: RefundExtraToFunderBox, 2: MiningFee
            val isOverFunded = allFundsOutput > repaymentGoal
            if (isOverFunded) {
                val refundExtraToFunderBox: Box             = OUTPUTS(1)

                val repaymentBoxFunded: Boolean             = outputSltRepaymentBox.tokens(1)._2 == repaymentGoal
                val extraFundsReturnedToFunder: Boolean     = {
                    allOf(Coll(
                        refundExtraToFunderBox.tokens(1)._2 == (allFundsOutput - repaymentGoal),
                        refundExtraToFunderBox.propositionBytes == _FunderPk
                    ))
                }

                sigmaProp(repaymentBoxFunded && extraFundsReturnedToFunder && fundable)
            } else {
                // ===== Fund Success ===== //
                // Input Boxes      -> 0: InputSltRepaymentBox, 1: SELF | PaymentBox
                // Output Boxes     -> 0: OutputSltRepaymentBox, 1: MiningFee
                val valueTransferred: Boolean       = outputSltRepaymentBox.tokens(1)._2 == allFundsOutput

                sigmaProp(fundable && valueTransferred)
            }

        } else {
        // ===== Failed Funding ===== //
            sigmaProp(false)
        }
    }
}