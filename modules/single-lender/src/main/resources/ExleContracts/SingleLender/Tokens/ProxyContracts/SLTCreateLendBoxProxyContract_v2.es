{
    // ===== Contract Info ===== //
    // Name             : SLT [Single Lender Tokens] Create Lend Box Proxy Contract
    // Description      : A contract to ensure that the lend box that is created has the
    //                    right information and accounting details in it.
    // Type             : Proxy Contract
    // Author           : Kii
    // Last Modified    : July 23rd 2022
    // Version          : v 2.0
    // Status           : Testing Phase

    // ===== Contract Hard-Coded Constants ===== //
    // val _MinFee:                         Long
    // val _SLTServiceNFTId:                Coll[Byte]
    // val _SLTLendTokenId:                 Coll[Byte]

    // ===== Contract Conditions ===== //
    // 1. Create Loan
    // 2. Refund

    // the amount of boxes as outputs, else return
    if (INPUTS.size == 1) {
        // ## Refund ##
        val inputPaymentBox: Box        = INPUTS(0)

        sigmaProp(
            allOf(Coll(
                OUTPUTS(0).value >= (INPUTS(0).value - _MinFee),
                OUTPUTS(0).propositionBytes == inputPaymentBox.R6[Coll[Byte]].get
            ))
        )
    } else {
        val isLenderPkDefined: Boolean  = OUTPUTS(1).R8[GroupElement].isDefined
        val inputPaymentBox: Box        = SELF

        sigmaProp(
            allOf(Coll(
                OUTPUTS(0).tokens(0)._1             == _SLTServiceNFTId,
                OUTPUTS(1).tokens(0)._1             == _SLTLendTokenId,
                OUTPUTS(1).R4[Coll[Long]].get       == inputPaymentBox.R4[Coll[Long]].get,
                OUTPUTS(1).R5[Coll[Coll[Byte]]].get == inputPaymentBox.R5[Coll[Coll[Byte]]].get,
                OUTPUTS(1).R6[Coll[Byte]].get       == inputPaymentBox.R6[Coll[Byte]].get,
                // Check if the loan token ID is correct
                OUTPUTS(1).R7[Coll[Byte]].get       == inputPaymentBox.R7[Coll[Byte]].get,
                OUTPUTS(1).value                    == _MinFee,
                !isLenderPkDefined
            ))
        )
    }
}