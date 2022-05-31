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
    // val _SLTServiceNFTId:            Coll[Byte]
    // val _SLTLendTokenId:             Coll[Byte]
    // val _SLTRepaymentTokenId:        Coll[Byte]

    // ===== Contract Conditions ===== //
    // 1. Fund Lend         - Fund the lend box when it is still fundable
    // 2. Fund Successful   - Loan has been successfully funded and is ready for the next step
    // 3. Refund Lend       - The lend box has existed past its deadline and the box is absorbed
    // 4. Mistakenly Funded - If the box was funded during creation (No lender and can't accept funds)
    //                        The box will be refunded back to the borrower.

    // ===== Contract Constants ===== //
    // #### Registers #### //
    val _fundingInfoRegister: Coll[Long]                = SELF.R4[Coll[Long]]
    val _projectDetailRegister: Coll[Coll[Byte]]        = SELF.R5[Coll[Coll[Byte]]]
    val _borrowerRegister: Coll[Byte]                   = SELF.R6[Coll[Byte]]
    val _loanTokenId: Coll[Byte]                        = SELF.R7[Coll[Byte]]
    val _lenderRegister: Coll[Byte]                     = SELF.R8[Coll[Byte]]

    // #### Others #### //
    val _fundingGoal: Long                              = _fundingInfoRegister.get(0)
    val _deadlineHeight: Long                           = _fundingInfoRegister.get(1)
    val _interestRate: Long                             = _fundingInfoRegister.get(2)
    val _repaymentHeightLength: Long                    = _fundingInfoRegister.get(3)

    // ===== Funded Successful ===== //
    // Description  : The funding is completed, and we're moving towards
    //              distributing the funds and converting the box to a
    //              repayment box
    // Input Boxes  : 0 -> SLTServiceBox, 1 -> SELF
    // Output Boxes : 0 -> SLTServiceBox, 1 -> SLTRepaymentBox, 2 -> FundsToBorrowerBox, 3 -> MiningFee
    //
    // Trigger Conditions:
    //      1. Loan hit funding goal
    //      2. Lender Register not empty

    val loanHitFundingGoal: Boolean             = SELF.tokens.length > 2 && SELF.tokens(1)._2 == _fundingGoal
    val lenderNonEmpty: Boolean                 = _lenderRegister.isDefined
    val isFunded: Boolean                       = loanHitFundingGoal && lenderNonEmpty

    if (isFunded)
    {
        // #### Boxes #### //
        val inputSLTServiceBox: Box                 = INPUTS(0)

        val outputSLTServiceBox: Box                = OUTPUTS(0)
        val outputSLTRepaymentBox: Box              = OUTPUTS(1)
        val fundsToBorrowerBox: Box                 = OUTPUTS(2)

        val loanToken: (Coll[Byte], Long)           = SELF.tokens(1)
        val repaymentDetails: Coll[Long]            = outputSLTRepaymentBox.R8[Coll[Long]]
        val repaymentBoxFundedHeight: Long          = repaymentDetails.get(0)
        val repaymentBoxRepaymentAmount: Long       = repaymentDetails.get(1)
        val repaymentBoxInterestRate: Long          = repaymentDetails.get(2)
        val repaymentBoxRepaymentHeightGoal: Long   = repaymentDetails.get(3)
        val totalInterestAmount: Long               = (_fundingGoal * _interestRate / 1000)

        // #### Condition Checks #### //
        val loanToBorrower: Boolean                 = {
            allOf(Coll(
                fundsToBorrowerBox.tokens(0)._1         == SELF.tokens(1)._1,
                fundsToBorrowerBox.tokens(0)._2         == SELF.tokens(1)._2,
                fundsToBorrowerBox.propositionBytes     == _borrowerRegister.get
            ))
        }

        val repaymentBoxCreation: Boolean           = {
            allOf(Coll(
                outputSLTRepaymentBox.tokens(0)._1      == _SLTRepaymentTokenId,
                outputSLTRepaymentBox.tokens(0)._2      == 1
            ))
        }

        val fundDetailsReplicated: Boolean          = {
            allOf(Coll(
                outputSLTRepaymentBox.R4[Coll[Long]]        == _fundingInfoRegister,
                outputSLTRepaymentBox.R5[Coll[Coll[Byte]]]  == _projectDetailRegister,
                outputSLTRepaymentBox.R6[Coll[Byte]]        == _borrowerRegister,
                outputSLTRepaymentBox.R7[Coll[Byte]]        == _loanTokenId,
                outputSLTRepaymentBox.R8[Coll[Byte]]        == _lenderRegister
            ))
        }

        val repaymentDetailCreated: Boolean         = {
            allOf(Coll(
                repaymentBoxFundedHeight        == CONTEXT.HEIGHT,
                repaymentBoxRepaymentAmount     == _fundingGoal + totalInterestAmount,
                repaymentBoxInterestRate        == totalInterestAmount,
                repaymentBoxRepaymentHeightGoal == repaymentBoxFundedHeight + _repaymentHeightLength
            ))
        }

        val serviceBoxVerification: Boolean         = inputSLTServiceBox.tokens(0)._1 == _SLTServiceNFTId

        sigmaProp(allOf(Coll(
            loanToBorrower,
            repaymentBoxCreation,
            fundDetailsReplicated,
            repaymentDetailCreated,
            serviceBoxVerification
        )))
    }
    else
    {
        // ===== Not Funded Actions ===== //

            // ===== Refund: Deadline Passed ===== //

            // ===== Mistakenly Funded: When Initiated ===== //

            // ===== Fund Actions ===== //

                // ===== Overfunded ===== //
                // Will it get overfunded?
    }
}