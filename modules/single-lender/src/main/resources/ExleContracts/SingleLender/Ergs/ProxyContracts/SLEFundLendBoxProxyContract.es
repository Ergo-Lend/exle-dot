{
    // ===== Contract Info ===== //
    // Name             : SLE [Single Lender Ergs] Fund Lend Box Proxy Contract
    // Description      : A contract to ensure that the funding goes to the right
    //                    lend box and if there is any overfunded amount, this script
    //                    ensures that the overfunded payment goes back to the lender.
    // Type             : Proxy Contract
    // Author           : Kii
    // Last Modified    : May 8th 2022
    // Version          : v 1.0
    // Status           : Completed

    // ===== Contract Hard-Coded Constants ===== //
    // val _BoxIdToFund:                    Coll[Byte]
    // val _LenderPk:                       Coll[Byte]
    // val _MinFee:                         Long
    // val _SLELendTokenId:                 Coll[Byte]

    // ===== Contract Conditions ===== //
    // 1. Fund Lend
    // 2. Refund

    // Refund
    if (INPUTS.size == 1) {
        // ** REFUND **
        val returnFunding = {
            allOf(Coll(
                OUTPUTS(0).value <= (INPUTS(0).value - _MinFee),
                OUTPUTS(0).propositionBytes == _LenderPk
            ))
        }

        sigmaProp(returnFunding)
    } else {
        // ** Variable Declaration **
        val inputLendBox = INPUTS(0)
        val inputPaymentBox = SELF
        val outputLendBox = OUTPUTS(0)

        val deadlineHeight = inputLendBox.R4[Coll[Long]].get(1)
        val fundingGoal = inputLendBox.R4[Coll[Long]].get(0)
        val lendBoxId = inputLendBox.id


        // ** Fund **
        val deadlineReached = deadlineHeight < HEIGHT
        val boxIdCheck = _BoxIdToFund == lendBoxId
        val fundable = boxIdCheck && !deadlineReached
        if (fundable) {

            val newFundedValue = inputLendBox.value + fundingGoal + _MinFee
            val isOverfunded = (SELF.value - newFundedValue) > 0

            val outputLendBoxLenderPk = outputLendBox.R7[Coll[Byte]]

            // -- Single Lender --
            //
            // Funds only happens once. Therefore must hit funding goal
            val fundLendBox = {
                allOf(Coll(
                      fundable,
                      outputLendBox.value == newFundedValue,
                      outputLendBox.value >= fundingGoal,
                      outputLendBoxLenderPk.get == _LenderPk,
                      inputLendBox.tokens(0)._1 == _SLELendTokenId,
                ))
            }

            if (isOverfunded) {
                val refundExtraBox = OUTPUTS(1)
                val overfundedCheck = {
                    allOf(Coll(
                        refundExtraBox.propositionBytes == _LenderPk,
                        refundExtraBox.value > (SELF.value - newFundedValue - _MinFee)
                    ))
                }
                sigmaProp(fundLendBox && overfundedCheck)
            } else {
                sigmaProp(fundLendBox)
            }
        } else {
            sigmaProp(false)
        }
    }
}