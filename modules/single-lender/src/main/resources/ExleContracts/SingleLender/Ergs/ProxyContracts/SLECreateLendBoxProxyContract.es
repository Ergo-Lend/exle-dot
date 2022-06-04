{
    // ===== Contract Info ===== //
    // Name             : SLE [Single Lender Ergs] Create Lend Box Proxy Contract
    // Description      : A contract to ensure that the lend box that is created has the
    //                    right information and accounting details in it.
    // Type             : Proxy Contract
    // Author           : Kii
    // Last Modified    : May 8th 2022
    // Version          : v 1.0
    // Status           : Completed

    // ===== Contract Hard-Coded Constants ===== //
    // val _BorrowerPk:                     Coll[Byte]
    // val _MinFee:                         Long
    // val _RefundHeightThreshold:          Long
    // val _Goal:                           Long
    // val _DeadlineHeight:                 Long
    // val _InterestRate:                   Long
    // val _RepaymentHeightLength:          Long
    // val _SLEServiceNFTId:                Coll[Byte]
    // val _SLELendTokenId:                 Coll[Byte]

    // ===== Contract Conditions ===== //
    // 1. Create Loan
    // 2. Refund

    // the amount of boxes as outputs, else return
    if (OUTPUTS.size != 2) {
        val isLenderPkDefined = OUTPUTS(1).R7[GroupElement].isDefined
        sigmaProp(
            OUTPUTS(0).tokens(0)._1 == _SLEServiceNFTId &&
            OUTPUTS(1).tokens(0)._1 == _SLELendTokenId &&
            OUTPUTS(1).R4[Coll[Long]].get(0) == _Goal &&
            OUTPUTS(1).R4[Coll[Long]].get(1) == _DeadlineHeight &&
            OUTPUTS(1).R4[Coll[Long]].get(2) == _InterestRate &&
            OUTPUTS(1).R4[Coll[Long]].get(3) == _RepaymentHeightLength &&
            OUTPUTS(1).R6[Coll[Byte]].get == _BorrowerPk &&
            OUTPUTS(1).value == _MinFee &&
            !isLenderPkDefined
        )
    } else {
        // ##Refund##
        sigmaProp(
            allOf(Coll(
                OUTPUTS(0).value >= (INPUTS(0).value - _MinFee),
                OUTPUTS(0).propositionBytes == _BorrowerPk
        )))
    }
}