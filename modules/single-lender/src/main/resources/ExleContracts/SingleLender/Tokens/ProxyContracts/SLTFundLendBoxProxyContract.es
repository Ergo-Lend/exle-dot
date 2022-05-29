{
    // ===== Contract Info ===== //
    // Name             : SLT [Single Lender Tokens] Fund Lend Box Proxy Contract
    // Description      : A contract to ensure that the funding goes to the right
    //                    lend box and if there is any overfunded amount, this script
    //                    ensures that the overfunded payment goes back to the lender.
    // Type             : Proxy Contract
    // Author           : Kii
    // Last Modified    : May 28th 2022
    // Version          : v 1.0
    // Status           : 1st Draft Completed

    // ===== Contract Hard-Coded Constants ===== //
    // val _BoxIdToFund:                    Coll[Byte]
    // val _LenderPk:                       Coll[Byte]
    // val _MinFee:                         Long
    // val _SLTLendTokenId:                 Coll[Byte]

    // ===== Contract Conditions ===== //
    // 1. Fund Lend
    // 2. Refund

    // ===== Refund ===== //
    // Description: Checks to see if the output box is of
    //              lender, and then returns the correct
    //              amount back to them.
    // Input Boxes      -> (0: PaymentBox)
    // Output Boxes     -> (0: RefundToLenderBox)
    val isRefund: Boolean = INPUTS.size == 1

    if (isRefund) {
        val tokenValueRefunded: Boolean = {
            allOf(Coll(
                OUTPUTS(0).tokens(0)._1     == INPUTS(0).tokens(0)._1,
                OUTPUTS(0).tokens(0)._2     == INPUTS(0).tokens(0)._2
            ))
        }
        val refundToLender: Boolean     = OUTPUTS(0).propositionBytes == _LenderPk,

        sigmaProp(tokenValueRefunded && refundToLender)
    } else {
        // ===== Variable Declaration ===== //
        val inputSltLendBox: Box                = INPUTS(0)
        val paymentBox: Box                     = SELF
        val outputSltLendBox: Box               = OUTPUTS(0)

        val deadlineHeight: Long        = inputSltLendBox.R4[Coll[Long]].get(1)
        val fundingGoal: Long           = inputSltLendBox.R4[Coll[Long]].get(0)
        val loanTokenId: Coll[Byte]     = inputSltLendBox.R7[Coll[Byte]].get
        val lendBoxId: Coll[Byte]       = inputLendBox.id

        val outputSltLendBoxLoanToken: (Coll[Byte], Long)   = outputSltLendBox.tokens(1)
        val paymentBoxToken: Coll[Byte]                     = paymentBox.tokens(0)

        // ===== Fundable ===== //
        // Description      : Funding is eligible
        // Input Boxes      -> (0: SLTLendBox, 1: PaymentBox)
        // Output Boxes     -> (0: SLTLendBox, [Optional: 1. RefundOverfundedToLenderBox])
        val deadlineReached: Boolean    = deadlineHeight < HEIGHT
        val boxIdCheck: Boolean         = _BoxIdToFund == lendBoxId
        val fundable: Boolean           = boxIdCheck && !deadlineReached

        if (fundable) {

            val isSltLendBox: Boolean   = inputSltLendBox.tokens(0)._1 == _SLTLendTokenId
            val lendBoxFunded: Boolean  = {
                allOf(Coll(
                    // token_value_funded
                    outputSltLendBoxLoanToken._2        == fundingGoal,
                    // token_id_correct
                    outputSltLendBoxLoanToken._1        == loanTokenId,
                    // extra_token_refund_box_is_lender
                    outputSltLendBoxLenderPk.get        == _LenderPk,
                ))
            }

            // ===== OverFunded ===== //
            // Description: Payment box has excessive funds that are required to be return
            //              to lender.
            // Input Boxes      -> (0: SLTLendBox, 1: PaymentBox)
            // Output Boxes     -> (0: SLTLendBox, 1: RefundOverfundedToLenderBox)
            val isOverFunded: Boolean   = (SELF.value - newFundedValue) > 0

            if (isOverfunded) {
                val refundExtraFundsToLenderBox: Box        = OUTPUTS(1)
                val overfundedCheck: Boolean                = {
                    allOf(Coll(
                        // extra_token_value_refunded
                        refundExtraFundsToLenderBox.tokens(0)._2        == SELF.tokens(0)._2 - fundingGoal,
                        // extra_token_id_same
                        refundExtraFundsToLenderBox.tokens(0)._1        == SELF.tokens(0)._1
                        // extra_token_refund_box_is_lender
                        refundExtraFundsToLenderBox.propositionBytes    == _LenderPk,
                    ))
                }

                sigmaProp(overfundedCheck && lendBoxFunded)
            } else {
                // ===== Fund Success ===== //
                // Input Boxes      -> (0: SLTLendBox, 1: PaymentBox)
                // Output Boxes     -> (0: SLTLendBox, 1: Mining Fee)
                sigmaProp(lendBoxFunded)
            }
        } else {
            // ===== Failed Funding ===== //
            sigmaProp(false)
        }
    }
}