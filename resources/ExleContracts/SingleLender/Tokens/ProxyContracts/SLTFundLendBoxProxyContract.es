{
    // ===== Contract Info ===== //
    // Name             : SLT [Single Lender Tokens] Fund Lend Box Proxy Contract
    // Description      : A contract to ensure that the funding goes to the right
    //                    lend box and if there is any overfunded amount, this script
    //                    ensures that the overfunded payment goes back to the lender.
    // Type             : Proxy Contract
    // Author           : Kii
    // Last Modified    : May 8th 2022

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
    val isRefund: Boolean = INPUTS.size == 1
    if (INPUTS.size == 1) {
        val tokenValueRefunded: Boolean = {
            allOf(Coll(
                OUTPUTS(0).tokens(0)._1 == INPUTS(0).tokens(0)._1,
                OUTPUTS(0).tokens(0)._2 == INPUTS(0).tokens(0)._2
            ))
        }

        val refundToLender: Boolean     = OUTPUTS(0).propositionBytes == _LenderPk,

        sigmaProp(tokenValueRefunded && refundToLender)
    } else {
        // ===== Variable Declaration ===== //
        val inputSltLendBox: Box        = INPUTS(0)
        val fundLendProxy: Box          = SELF
        val outputSltLendBox: Box       = OUTPUTS(0)

        val deadlineHeight: Long        = inputSltLendBox.R4[Coll[Long]].get(1)
        val fundingGoal: Long           = inputSltLendBox.R4[Coll[Long]].get(0)
        val lendBoxId: Coll[Byte]       = inputLendBox.id


        // ===== Fund ===== //
        val deadlineReached: Boolean    = deadlineHeight < HEIGHT
        val boxIdCheck: Boolean         = _BoxIdToFund == lendBoxId
        val fundable: Boolean           = boxIdCheck && !deadlineReached
        if (fundable) {

            if (isOverfunded) {
            // ===== OverFunded ===== //

            } else {
            // ===== Fund Success ===== //
            }

        } else {
        // ===== Failed Funding ===== //
            sigmaProp(false)
        }
    }
}