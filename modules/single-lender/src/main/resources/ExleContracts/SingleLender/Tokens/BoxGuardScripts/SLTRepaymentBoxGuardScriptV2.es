{
    // ===== Contract Info ===== //
    // Name             : SLT [Single Lender Tokens] Repayment Box Guard Script
    // Description      : A single lender repayment box allows borrowers or other interested parties
    //                    to fund a repayment box that is meant to be returned to the lender who
    //                    lent his funds to the borrower. This box ensures that the funds are returned
    //                    to the lender when the repayment amount is reached.
    // Type             : Guard Script
    // Author           : Kii
    // Last Modified    : July 17th 2022
    // Version          : v 2.0
    // Version Updates  : 1. Repayment Box is kept as historical records and is unspendable
    //                    2. Repayment Box distributes funds when there is enough funds to be distributed
    //                    3. Default is handled by other components. The repayment box is always fundable
    //                      as long as it hasn't reached it's funding goal.
    // Status           : Implementation

    // ===== Contract Hard-Coded Constants ===== //
    // val _MinFee:                     Long
    // val _SLTServiceBoxNFTId:         Coll[Byte]
    // val _SLTRepaymentTokenId:        Coll[Byte]

    // ===== Contract Conditions ===== //
    // 1. Fund Repayment        - Fund the repayment box when it is still fundable.
    //                            Also processes over funded boxes
    // 2. Distribute Repayment  - When Repayment is fully funded, the box ensures that
    //                            the funds are returned to the lender.
    //
    // ===== Notes ===== //
    // 1. Funded Unspendable - When a repayment is fully funded, it becomes a historical records and is unspendable
    // 2. Defaulting Credit Check - Defaulting affects credit check (to be implemented), not the state of the box

    // ===== Contract Constants ===== //
    // ##### Values ##### //
    val _fundingInfoRegister: Coll[Long]            = SELF.R4[Coll[Long]]
    val _projectDetailRegister: Coll[Coll[Byte]]    = SELF.R5[Coll[Coll[Byte]]]
    val _borrowerRegister: Coll[Byte]               = SELF.R6[Coll[Byte]]
    val _loanTokenId: Coll[Byte]                    = SELF.R7[Coll[Byte]]
    val _lenderRegister: Coll[Byte]                 = SELF.R8[Coll[Byte]]
    val _repaymentDetailsRegister: Coll[Long]       = SELF.R9[Coll[Long]]

    val _interestRate: Long                         = _fundingInfoRegister.get(2)
    val _repaymentAmountGoal: Long                  = _repaymentDetailsRegister.get(1)
    val _totalInterestAmount: Long                  = _repaymentDetailsRegister.get(2)
    val _repaymentHeightGoal: Long                  = _repaymentDetailsRegister.get(3)
    val _totalRepaidAmount: Long                    = _repaymentDetailsRegister.get(4)
    val _percentageDenominator: Long                = 1000

    val outSLTRepaymentBox: Box                     = OUTPUTS(0)
    // ##### Global Conditions ##### //
    val isRepaymentDetailsReplicated: Boolean       = {
        allOf(Coll(
            outSLTRepaymentBox.R4[Coll[Long]].get       == SELF.R4[Coll[Long]].get,
            outSLTRepaymentBox.R5[Coll[Coll[Byte]]].get == SELF.R5[Coll[Coll[Byte]]].get,
            outSLTRepaymentBox.R6[Coll[Byte]].get       == SELF.R6[Coll[Byte]].get,
            outSLTRepaymentBox.R7[Coll[Byte]].get       == SELF.R7[Coll[Byte]].get,
            outSLTRepaymentBox.R8[Coll[Byte]].get       == SELF.R8[Coll[Byte]].get,
            outSLTRepaymentBox.R9[Coll[Long]].get       == SELF.R9[Coll[Long]].get,
        ))
    }

    // ====== Conditions ====== //
    // 1. Fund Repayment - Has 2 Inputs: 0. RepaymentBox, 1. FundPaymentBox
    // 2. Distribute Repayment - Has 1 Inputs: 0. RepaymentBox

    val isDistributeRepayment: Boolean      = INPUTS.length == 1
    if (isDistributeRepayment) {
        // ====== Distribute Repayment ===== //
        // Description  : Distribute repayment to lender and protocol owner
        // INPUTS       : 0. SLTRepaymentBox(SELF)
        // OUTPUTS      : 0. OutSLTRepaymentBox, 1. FundsToLenderBox, 2. FundsToProtocolOwnerBox, 3. MiningFee
        // DataInputs   : 0. SLTServiceBox
        // Conditions   : 1. IsRepaymentDetailsReplicated
        //                2. IsRepaidAmountUpdated
        //                3. IsLenderFunded - Checks the output box
        //                4. IsProtocolOwnerFunded - Checks the output box

        val fundsToLenderBox: Box                   = OUTPUTS(1)
        val fundsToProtocolOwnerBox: Box            = OUTPUTS(2)
        val sltServiceBoxAsDataInput: Box           = CONTEXT.dataInputs(0)
        val protocolOwnerAddress: Coll[Byte]        = sltServiceBoxAsDataInput.R7[Coll[Byte]]
        val profitSharingRegister: Coll[Long]       = sltServiceBoxAsDataInput.R8[Coll[Long]]

        val totalRepaymentBoxTokenAmount: Long     =
            if (SELF.tokens.size > 1) {
                // If this does not pass, do 0 + SELF.tokens(1)._2
                SELF.tokens(1)._2
            } else 0

        // Capital = Repayment * (Denominator / (Denominator + Interest Rate))
        // Interest = Repayment - Capital
        // ProfitSharing = Interest * ProfitSharingRate
        val repaymentAmount: Long                   = totalRepaymentBoxTokenAmount
        val capitalAmount: Long                     = (repaymentAmount * _percentageDenominator) / (_percentageDenominator + _interestRate)
        val interest: Long                          = repaymentAmount - capitalAmount
        val profitSharingAmount: Long               = (interest * profitSharingRegister.get(0)) / _percentageDenominator

        // ##### IsRepaidAmountUpdated ##### //
        val _outTotalRepaidAmount: Long     = outSLTRepaymentBox.R9[Coll[Long]].get(4)

        val isRepaidAmountUpdated: Boolean  = _outTotalRepaidAmount == _totalRepaidAmount + totalInputTokenAmount

        // ##### IsLenderFunded ##### //
        // 1. isLoanTokenForLenderBox
        // 2. isLenderAddressForLenderBox
        // 3. isCorrectSplitAmountForLenderBox
        val lenderReceivedToken: (Coll[Byte], Long)         = fundsToLenderBox.tokens(0)

        val isLoanTokenForLenderBox: Boolean                =
            lenderReceivedToken._1 == _loanTokenId.get,

        val isLenderAddressForLenderBox: Boolean            =
            fundsToLenderBox.propositionBytes == _lenderRegister.get,

        val paymentAmountToLender: Long                     = capitalAmount + (interest - profitSharingAmount)
        val isCorrectSplitAmountForLenderBox: Boolean       =
            lenderReceivedToken._2 == paymentAmountToLender


        val isLenderFunded: Boolean                         = allOf(Coll(
            isLoanTokenForLenderBox,
            isLenderAddressForLenderBox,
            isCorrectSplitAmountForLenderBox
        ))

        // ##### IsProtocolOwnerFunded ##### //
        // 1. isLoanTokenForProtocolOwnerBox
        // 2. isProtocolOwnerAddressForProtocolOwnerBox
        // 3. isCorrectProfitSharingAmountForProtocolOwnerBox
        val protocolOwnerReceivedToken: (Coll[Byte], Long)  = fundsToProtocolOwnerBox.tokens(0)

        val isLoanTokenForProtocolOwnerBox: Boolean                     =
            protocolOwnerReceivedToken._1 == _loanTokenId.get

        val isProtocolOwnerAddressForProtocolOwnerBox: Boolean          =
            fundsToProtocolOwnerBox.propositionBytes == protocolOwnerAddress

        val isCorrectProfitSharingAmountForProtocolOwnerBox: Boolean    =
            protocolOwnerReceivedToken._2 == profitSharingAmount

        val isProtocolOwnerFunded: Boolean                  = allOf(Coll(
            isLoanTokenForProtocolOwnerBox,
            isProtocolOwnerAddressForProtocolOwnerBox,
            isCorrectProfitSharingAmountForProtocolOwnerBox
        ))

        sigmaProp(allOf(Coll(
            isRepaymentDetailsReplicated,
            isRepaidAmountUpdated,
            isLenderFunded,
            isProtocolOwnerFunded
        )))
    } else {
        // ====== Fund Repayment ====== //
        // Description  : Increment funding in SLTRepaymentBox. RepaymentBox when being funded should end
        //                  up with ErgValue of (Parameters.MinFee * 4). This is to ensure distribution is
        //                  possible. If the box already have that amount, we don't bother to add. Else,
        //                  it is mandatory to fund that amount.
        // Scenarios    : 1. OverFunded
        //                2. Fund
        val paymentBox: Box                 = INPUTS(1)

        val totalInputTokenAmount: Long     =
            if (SELF.tokens.size > 1) {
                paymentBox.tokens(0)._2 + SELF.tokens(1)._2
            } else paymentBox.tokens(0)._2

        // ##### Global Fund Conditions ##### //
        // 1. IsEnoughErgsForDistribution
        // 2. IsPaymentBoxHasRightToken
        val isEnoughErgsForDistribution: Boolean            = outSLTRepaymentBox.value == (_MinFee * 4)
        val isPaymentBoxHasRightToken: Boolean              = true
        val isRepaymentNotFullyFunded: Boolean            =
            if (SELF.tokens.size > 1) {
                SELF.tokens(0)._1 == _SLTRepaymentTokenId && SELF.tokens(1)._1 == _loanTokenId.get && SELF.tokens(1)._2 < _repaymentAmountGoal
            }
            else {
                SELF.tokens(0)._1 == _SLTRepaymentTokenId
            }

        val isGlobalFundConditions: Boolean                 = allOf(Coll(
            isEnoughErgsForDistribution,
            isPaymentBoxHasRightToken,
            isRepaymentDetailsReplicated,
            isRepaymentNotFullyFunded
        ))

        val totalFundedValueMoreThanRepaymentGoal: Boolean  = totalInputTokenAmount > _repaymentAmountGoal
        val isOverFunded: Boolean                           = totalFundedValueMoreThanRepaymentGoal
        if (isOverFunded) {
            // ====== Overfunded ====== //
            // Description  : Increment funding in SLTRepaymentBox
            // INPUTS       : 0. SLTRepaymentBox(SELF), 1. FundPaymentBox
            // OUTPUTS      : 0. outSLTRepaymentBox, 1. ReturnToFunderBox, 2. MiningFee
            // Conditions   : 1. IsReturnToFunderBox
            //                2. IsRepaymentBoxFundedIsRepaymentGoal

            val returnToFunderBox: Box                          = OUTPUTS(1)
            val isReturnToFunderBox: Boolean                    = allOf(Coll(
                returnToFunderBox.tokens(0)._2      == totalInputTokenAmount - (_repaymentAmountGoal - _totalRepaidAmount),
                returnToFunderBox.tokens(0)._1      == _loanTokenId.get
            ))

            val isRepaymentBoxFundedIsRepaymentGoal: Boolean    = allOf(Coll(
                outputRepaymentBox.tokens(1)._2     == _repaymentAmountGoal - _totalRepaidAmount,
                outputRepaymentBox.tokens(1)._1     == _loanTokenId.get
            ))

            sigmaProp(allOf(Coll(
                isReturnToFunderBox,
                isRepaymentBoxFundedIsRepaymentGoal,
                isGlobalFundConditions
            )))
        } else {
            // ====== Fund ====== //
            // Description  : Increment funding in SLTRepaymentBox.
            //                  This should take into account ->
            //                  1. Funding via tokens
            //                  2. Funding via Erg for box spending
            // INPUTS       : 0. SLTRepaymentBox(SELF), 1. FundPaymentBox
            // OUTPUTS      : 0. outSLTRepaymentBox, 1. MiningFee
            // Conditions   : 1. IsValueTransferred

            val isValueTransferred: Boolean             = allOf(Coll(
                outSLTRepaymentBox.tokens(1)._2     == totalInputTokenAmount,
                outSLTRepaymentBox.tokens(1)._1     == _loanTokenId.get
            ))

            sigmaProp(allOf(Coll(
                isValueTransferred,
                isGlobalFundConditions
            )))
        }
    }
}