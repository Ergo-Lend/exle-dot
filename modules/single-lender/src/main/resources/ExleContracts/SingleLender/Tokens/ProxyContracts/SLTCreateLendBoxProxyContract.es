{
    // ===== Contract Info ===== //
    // Name             : SLT [Single Lender Tokens] Create Lend Box Proxy Contract
    // Description      : A contract to ensure that the lend box that is created has the
    //                    right information and accounting details in it.
    // Type             : Proxy Contract
    // Author           : Kii
    // Last Modified    : May 28th 2022
    // Version          : v 1.0
    // Status           : Testing Phase

    // ===== Contract Hard-Coded Constants ===== //
    // val _BorrowerPk:                     Coll[Byte]
    // val _LoanTokenId:                    Coll[Byte]
    // val _MinFee:                         Long
    // val _RefundHeightThreshold:          Long
    // val _Goal:                           Long
    // val _DeadlineHeight:                 Long
    // val _InterestRate:                   Long
    // val _RepaymentHeightLength:          Long
    // val _SLTServiceNFTId:                Coll[Byte]
    // val _SLTLendTokenId:                 Coll[Byte]

    // ===== Contract Conditions ===== //
    // 1. Create Loan
    // 2. Refund

    // the amount of boxes as outputs, else return
    if (OUTPUTS.size != 2) {
        val isLenderPkDefined: Boolean = OUTPUTS(1).R8[GroupElement].isDefined

        sigmaProp(
            allOf(Coll(
                OUTPUTS(0).tokens(0)._1             == _SLTServiceNFTId,
                OUTPUTS(1).tokens(0)._1             == _SLTLendTokenId,
                OUTPUTS(1).R4[Coll[Long]].get(0)    == _Goal,
                OUTPUTS(1).R4[Coll[Long]].get(1)    == _DeadlineHeight,
                OUTPUTS(1).R4[Coll[Long]].get(2)    == _InterestRate,
                OUTPUTS(1).R4[Coll[Long]].get(3)    == _RepaymentHeightLength,
                OUTPUTS(1).R6[Coll[Byte]].get       == _BorrowerPk,
                // Check if the loan token ID is correct
                OUTPUTS(1).R7[Coll[Byte]].get       == _LoanTokenId,
                OUTPUTS(1).value                    == _MinFee,
                !isLenderPkDefined
            ))
        )
    } else {
        // ## Refund ##
        sigmaProp(
            allOf(Coll(
                OUTPUTS(0).value >= (INPUTS(0).value - _MinFee),
                OUTPUTS(0).propositionBytes == _BorrowerPk
            ))
        )
    }
}