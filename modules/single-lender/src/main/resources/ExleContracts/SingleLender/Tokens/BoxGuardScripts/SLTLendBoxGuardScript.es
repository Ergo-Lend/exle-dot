{
    // ===== Contract Info ===== //
    // Name             : SLT [Single Lender Tokens] Lend Box Guard Script
    // Description      : A single lender lend box allows interested lenders to participate
    //                    in the activity of lending to a borrower. The box ensures that
    //                    all value within the box is funded to the borrower and borrower
    //                    only. It also ensures that a repayment box is created upon successful
    //                    lending.
    // Type             : Guard Script
    // Author           : Kii
    // Last Modified    : May 8th 2022
    // Version          : v 1.0
    // Status           : In Progress

    // ===== Contract Hard-Coded Constants ===== //
    // val _MinFee:                     Long
    // val _MinBoxAmount:               Long
    // val _SLEServiceNFTId:            Coll[Byte]
    // val _SLELendTokenId:             Coll[Byte]
    // val _SLERepaymentTokenId:        Coll[Byte]

    // ===== Contract Conditions ===== //
    // 1. Fund Lend         - Fund the lend box when it is still fundable
    // 2. Fund Successful   - Loan has been successfully funded and is ready for the next step
    // 3. Refund Lend       - The lend box has existed past its deadline and the box is absorbed
    // 4. Mistakenly Funded - If the box was funded during creation (No lender and can't accept funds)
    //                        The box will be refunded back to the borrower.

    // ===== Funded Successful ===== //

    // ===== Not Funded Actions ===== //

        // ===== Refund: Deadline Passed ===== //

        // ===== Mistakenly Funded: When Initiated ===== //

        // ===== Fund Actions ===== //

            // ===== Overfunded ===== //
            // Will it get overfunded?
}