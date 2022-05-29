{
    // ===== Contract Info ===== //
    // Name             : SLT [Single Lender Tokens] Repayment Box Guard Script
    // Description      : A single lender repayment box allows borrowers or other interested parties
    //                    to fund a repayment box that is meant to be returned to the lender who
    //                    lent his funds to the borrower. This box ensures that the funds are returned
    //                    to the lender when the repayment amount is reached.
    // Type             : Guard Script
    // Author           : Kii
    // Last Modified    : May 8th 2022
    // Version          : v 1.0
    // Status           : In Progress

    // ===== Contract Hard-Coded Constants ===== //
    // val _MinFee:                     Long
    // val _SLEServiceBoxNFTId:         Coll[Byte]
    // val _SLERepaymentTokenId:        Coll[Byte]

    // ===== Contract Conditions ===== //
    // 1. Fund Repayment        - Fund the repayment box when it is still fundable.
    //                            Also processes overfunded boxes
    // 2. Process Repayment     - When Repayment is fully funded, the box ensures that
    //                            the funds are returned to the lender.

    // ===== Not Funded Actions ===== //

        // ===== Overfunded ===== //

        // ===== Fund ===== //

    // ===== Funded Actions ===== //

        // ===== Fully Funded: Fund Success ===== //

        // ===== Defaulted ===== //

        // ===== Fail it ===== //
}