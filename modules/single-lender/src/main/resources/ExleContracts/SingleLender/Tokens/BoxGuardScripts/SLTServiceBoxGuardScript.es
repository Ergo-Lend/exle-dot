{
    // ===== Contract Info ===== //
    // Name             : SLT [Single Lender Tokens] Service Box Guard Script
    // Description      : A single lender service box manages the boxes that are run throughout
    //                    the SLE system. This will ensure that lend and repayment boxes receives
    //                    their identification tokens. It ensures that the Exle DAO is paid through
    //                    service fees and interest cut.
    // Type             : Guard Script
    // Author           : Kii
    // Last Modified    : May 8th 2022
    // Version          : v 1.0
    // Status           : In Progress

    // ===== Contract Hard-Coded Constants ===== //
    // val _MinFee:                     Long
    // val _OwnerPk:                    Coll[Byte]
    // val _LendBoxHash:                Digest32
    // val _RepaymentBoxHash:           Digest32

    // ===== Contract Conditions ===== //
    // 1. Mutating Service Box          - Mutate Service box with new logic
    // 2. Lend Initiation               - Initialized/create a loan box
    // 3. Lend to Repayment             - Lend box succeeds in funding, and is converted to Repayment box
    // 4. Repayment Absorption          - Absorbing repayment box (may be defaulted or completed)
    //      a. Defaulted | Zero Interest        - As stated
    //      b. Completed                        - Repayment Success
    // 5. Lend Box Absorption           - Absorbing a lend box due to inactivity

    // ===== Service Check ===== //

    // ===== Mutating service box ===== //

        // ===== Lend Initiation ===== //

        // ===== Lend to Repayment (Lend Success) ===== //

        // ===== Repayment Absorption ===== //

            // ===== Defaulted | Zero Interest Loans ===== //

        // ===== Refund Lend Box ===== //
}
