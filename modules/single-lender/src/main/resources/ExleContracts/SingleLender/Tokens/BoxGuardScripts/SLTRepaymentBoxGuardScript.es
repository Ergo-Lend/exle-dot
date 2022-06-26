{
    // ===== Contract Info ===== //
    // Name             : SLT [Single Lender Tokens] Repayment Box Guard Script
    // Description      : A single lender repayment box allows borrowers or other interested parties
    //                    to fund a repayment box that is meant to be returned to the lender who
    //                    lent his funds to the borrower. This box ensures that the funds are returned
    //                    to the lender when the repayment amount is reached.
    // Type             : Guard Script
    // Author           : Kii
    // Last Modified    : June 4th 2022
    // Version          : v 1.0
    // Status           : 1st Draft without default

    // ===== Contract Hard-Coded Constants ===== //
    // val _MinFee:                     Long
    // val _SLTServiceBoxNFTId:         Coll[Byte]
    // val _SLTRepaymentTokenId:        Coll[Byte]

    // ===== Contract Conditions ===== //
    // 1. Fund Repayment        - Fund the repayment box when it is still fundable.
    //                            Also processes overfunded boxes
    // 2. Process Repayment     - When Repayment is fully funded, the box ensures that
    //                            the funds are returned to the lender.

    // ===== Contract Constants ===== //
    // ##### Values ##### //
    val _fundingInfoRegister: Coll[Long]            = SELF.R4[Coll[Long]]
    val _projectDetailRegister: Coll[Coll[Byte]]    = SELF.R5[Coll[Coll[Byte]]]
    val _borrowerRegister: Coll[Byte]               = SELF.R6[Coll[Byte]]
    val _loanTokenId: Coll[Byte]                    = SELF.R7[Coll[Byte]]
    val _lenderRegister: Coll[Byte]                 = SELF.R8[Coll[Byte]]
    val _repaymentDetailsRegister: Coll[Long]       = SELF.R9[Coll[Long]]

    val _repaymentAmountGoal: Long                  = _repaymentDetailsRegister.get(1)
    val _totalInterestAmount: Long                  = _repaymentDetailsRegister.get(2)
    val _repaymentHeightGoal: Long                  = _repaymentDetailsRegister.get(3)

    // ##### Conditions ##### //
    val RepaymentTokensCorrect: Boolean             = SELF.tokens(1)._1 == _loanTokenId.get
    val ServiceBoxVerification: Boolean             = INPUTS(0).tokens(0)._1 == _SLTServiceBoxNFTId
    val RepaymentNotFullyFunded: Boolean            = SELF.tokens(1)._2 < _repaymentAmountGoal

    // ===== Not Funded Actions ===== //
    // Description  : Repayment box is not funded yet, therefore
    //              we go into funding mode
    if (RepaymentNotFullyFunded && !ServiceBoxVerification)
    {
        // ##### Not Funded Constants ##### //
        // ##### Boxes ##### //
        val paymentBox: Box                 = INPUTS(1)
        val outputRepaymentBox: Box         = OUTPUTS(0)

        // ##### Values ##### //
        val totalInputTokenAmount: Long     = paymentBox.tokens(0)._2 + SELF.tokens(1)._2

        // ##### Conditions ##### //
        val paymentBoxIsRightToken: Boolean     = paymentBox.tokens(0)._1 == _loanTokenId.get
        val repaymentBoxDetailCheck: Boolean    = {
            allOf(Coll(
                outputRepaymentBox.R4[Coll[Long]].get        == SELF.R4[Coll[Long]].get,
                outputRepaymentBox.R5[Coll[Coll[Byte]]].get  == SELF.R5[Coll[Coll[Byte]]].get,
                outputRepaymentBox.R6[Coll[Byte]].get        == SELF.R6[Coll[Byte]].get,
                outputRepaymentBox.R7[Coll[Byte]].get        == SELF.R7[Coll[Byte]].get,
                outputRepaymentBox.R8[Coll[Byte]].get        == SELF.R8[Coll[Byte]].get,
                outputRepaymentBox.R9[Coll[Long]].get        == SELF.R9[Coll[Long]].get
            ))
        }

        // ===== Overfunded ===== //
        // Description  : The payment box has more funds than needed.
        //              We fill the repayment box then return the rest
        //              of the payment box.
        // Input Boxes  : 0 -> SELF, 1 -> PaymentBox
        // Output Boxes : 0 -> OutputRepaymentBox, 1 -> ReturnToFunderBox, 2 -> MiningFee
        val totalFundedValueMoreThanRepaymentGoal: Boolean  = totalInputTokenAmount > _repaymentAmountGoal
        val isOverfunded: Boolean                           = totalFundedValueMoreThanRepaymentGoal
        if (isOverfunded)
        {
            val returnToFunderBox: Box      = OUTPUTS(1)

            val repaymentBoxFundedIsRepaymentGoal: Boolean  = {
                allOf(Coll(
                    outputRepaymentBox.tokens(1)._2     == _repaymentAmountGoal,
                    outputRepaymentBox.tokens(1)._1     == _loanTokenId.get
                ))
            }

            val returnToFunderBoxCorrectAmount: Boolean = {
                allOf(Coll(
                    returnToFunderBox.tokens(0)._2      == totalInputTokenAmount - _repaymentAmountGoal,
                    returnToFunderBox.tokens(0)._1      == _loanTokenId.get
                ))
            }

            sigmaProp(allOf(Coll(
                repaymentBoxFundedIsRepaymentGoal,
                returnToFunderBoxCorrectAmount,
                RepaymentNotFullyFunded,
                repaymentBoxDetailCheck
            )))
        }
        else
        {
            // ===== Fund ===== //
            // Description  : Fund the box with the value that it the payment box has
            // Input Boxes  : 0 -> SELF, 1 -> PaymentBox
            // Output Boxes : 0 -> OutputRepaymentBox, 1 -> MiningFee
            val valueTransferred: Boolean       = {
                allOf(Coll(
                    outputRepaymentBox.tokens(1)._2     == totalInputTokenAmount,
                    outputRepaymentBox.tokens(1)._1     == _loanTokenId.get
                ))
            }

            sigmaProp(allOf(Coll(
                valueTransferred,
                paymentBoxIsRightToken,
                RepaymentNotFullyFunded,
                repaymentBoxDetailCheck
            )))
        }
    }
    else
    {
        // ===== Funded Actions ===== //
        // ##### Box ##### //
        val inputServiceBox: Box           = INPUTS(0)

        // ===== Fully Funded: Fund Success ===== //
        // Description  : Fund is successful, therefore we convert it to
        //              repayment box
        // Input Boxes  : 0 -> InputSLTServiceBox, 1 -> SELF
        // Output Boxes : 0 -> OutputSLTServiceBox, 1 -> ProfitSharingBox, 2 -> RepaymentToLenderBox, 3 -> MiningFee
        val repaymentFundingGoalReached: Boolean    = SELF.tokens(1)._2 == _repaymentAmountGoal

        val fullyFunded: Boolean                    = repaymentFundingGoalReached
        if (fullyFunded)
        {
            // ##### Constants ##### //
            val profitSharingValue: Long            = _totalInterestAmount * (inputServiceBox.R8[Coll[Long]].get(0) / 1000)
            val repaymentToLenderValue: Long        = _repaymentAmountGoal - profitSharingValue
            // ##### Box ##### //
            val repaymentToLenderBox: Box           = OUTPUTS(2)

            val repaymentToLenderBoxFunded: Boolean = {
                allOf(Coll(
                    repaymentToLenderBox.tokens(0)._1       == _loanTokenId.get,
                    repaymentToLenderBox.tokens(0)._2       == repaymentToLenderValue,
                    repaymentToLenderBox.propositionBytes   == _lenderRegister.get
                ))
            }

            sigmaProp(allOf(Coll(
                repaymentToLenderBoxFunded,
                ServiceBoxVerification
            )))
        }
        else
        {
            // ===== Defaulted ===== //
            // Description  : Box is defaulted, lender can retrieve the funds, but will have
            //              the box kept so that default is kept on chain
            // Input Boxes  : 0 -> InputSLTServiceBox, 1 -> SELF, 2 -> LenderTriggerBox
            // Output Boxes : 0 -> OutputSLTServiceBox, 1 -> OutputRepaymentBox, 2 -> RepaymentToLenderBox, 3 -> MiningFee
            //
            // @todo kelim Leaving this open so that we can think about what
            // we should do for default.
            val repaymentHeightGoalPassed: Boolean      = CONTEXT.HEIGHT > _repaymentHeightGoal
            if (repaymentHeightGoalPassed)
            {
                sigmaProp(true)
            }
            else
            {
                // ===== Fail it ===== //
                sigmaProp(false)
            }
        }
    }
}