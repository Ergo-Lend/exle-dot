{
    // ===== Contract Info ===== //
    // Name             : SLE [Single Lender Ergs] Repayment Box Guard Script
    // Description      : A single lender repayment box allows borrowers or other interested parties
    //                    to fund a repayment box that is meant to be returned to the lender who
    //                    lent his funds to the borrower. This box ensures that the funds are returned
    //                    to the lender when the repayment amount is reached.
    // Type             : Guard Script
    // Author           : Kii
    // Last Modified    : May 8th 2022
    // Version          : v 1.0
    // Status           : Completed

    // ===== Contract Hard-Coded Constants ===== //
    // val _MinFee:                     Long
    // val _SLEServiceBoxNFTId:         Coll[Byte]
    // val _SLERepaymentTokenId:        Coll[Byte]

    // ===== Contract Conditions ===== //
    // 1. Fund Repayment        - Fund the repayment box when it is still fundable.
    //                            Also processes overfunded boxes
    // 2. Process Repayment     - When Repayment is fully funded, the box ensures that
    //                            the funds are returned to the lender.

    val repaymentBoxVerification = SELF.tokens(0)._1 == _SLERepaymentTokenId
    val serviceBox = INPUTS(0)
    val serviceBoxVerification = INPUTS(0).tokens(0)._1 == _SLEServiceBoxNFTId

    // If 1st Output's propositionBytes is the same, then its a repayment box
    // INPUTS(0) and OUTPUTS(0) can be true during fund success
    val isRepaymentBox = INPUTS(0).propositionBytes == OUTPUTS(0).propositionBytes
    if (isRepaymentBox && !serviceBoxVerification) {
        // *** ADDING FUNDS ***
        // RepaymentBox, ProxyContract -> OutRepaymentBox
        // @variables _MinFee

        val repaymentBoxInput = SELF
        val repaymentBoxOutput = OUTPUTS(0)
        val proxyBox = INPUTS(1)

        val inputRepaymentBoxAccounting = repaymentBoxInput.R4[Coll[Long]]
        val inputRepaymentBoxDetails = repaymentBoxInput.R5[Coll[Coll[Byte]]]
        val inputRepaymentBoxBorrower = repaymentBoxInput.R6[Coll[Byte]]
        val inputRepaymentBoxLender = repaymentBoxInput.R7[Coll[Byte]]
        val inputRepaymentBoxRepaymentDetails = repaymentBoxInput.R8[Coll[Long]]

        val outputRepaymentBoxAccounting = repaymentBoxOutput.R4[Coll[Long]]
        val outputRepaymentBoxDetails = repaymentBoxOutput.R5[Coll[Coll[Byte]]]
        val outputRepaymentBoxBorrower = repaymentBoxOutput.R6[Coll[Byte]]
        val outputRepaymentBoxLender = repaymentBoxOutput.R7[Coll[Byte]]
        val outputRepaymentBoxRepaymentDetails = repaymentBoxOutput.R8[Coll[Long]]

        val repaymentBoxDetailCheck = {
            allOf(Coll(
                repaymentBoxVerification,
                outputRepaymentBoxAccounting == inputRepaymentBoxAccounting,
                outputRepaymentBoxDetails == inputRepaymentBoxDetails,
                outputRepaymentBoxBorrower == inputRepaymentBoxBorrower,
                outputRepaymentBoxLender == inputRepaymentBoxLender,
                outputRepaymentBoxRepaymentDetails == inputRepaymentBoxRepaymentDetails
            ))
        }

        val repaymentNotFullyFunded = repaymentBoxInput.value < SELF.R8[Coll[Long]].get(1)
        val transferredValue = repaymentBoxInput.value + proxyBox.value - _MinFee
        val isValueTransferred = repaymentBoxOutput.value == transferredValue
        val repaymentGoal = inputRepaymentBoxRepaymentDetails.get(1)

        /**
        * When overfunded
        *
        * We fill the repayment box and return the rest to the funder
        * ErgoLend's proxy contract ensures the funds get returned to
        * funder. This is because anyone can fund a repayment, and the
        * funds should return to its rightful owner.
        **/
        val overfunded = transferredValue > repaymentGoal
        if (overfunded) {
            val goaledRepaymentBox = repaymentBoxOutput.value == repaymentGoal + _MinFee
            val repaymentGoalReached = {
                allOf(Coll(
                    repaymentBoxDetailCheck,
                    repaymentNotFullyFunded,
                    goaledRepaymentBox
                ))
            }

            sigmaProp(repaymentGoalReached)
        } else {
            val repaymentFundingFulfilled = {
                allOf(Coll(
                    isValueTransferred,
                    repaymentBoxDetailCheck,
                    repaymentNotFullyFunded
                ))
            }

            sigmaProp(repaymentFundingFulfilled)
        }
    } else {

        // Scenario 3: Fully funded
        // Spend to return to lender
        // ServiceBox, RepaymentBox -> ServiceBox, ProfitSharing, LenderFundedBox
        // *** REPAYMENT FULFILLED ***

        val selfRepaymentBox = SELF
        val selfFundingDetails = SELF.R4[Coll[Long]]
        val selfLendDetails = SELF.R5[Coll[Coll[Byte]]]
        val selfBorrowerPk = SELF.R6[Coll[Byte]]
        val selfLenderPk = SELF.R7[Coll[Byte]]
        val selfRepaymentDetails = SELF.R8[Coll[Long]]

        val fundingSuccessful = selfRepaymentBox.value >= selfRepaymentDetails.get(1)
        if (fundingSuccessful) {

            val inputRepaymentBox = SELF
            val lenderRepaidBox = OUTPUTS(1)

            // compare lender's box and value
            val inputRepaymentAcc = inputRepaymentBox.R4[Coll[Long]]
            val fundingGoal = inputRepaymentAcc.get(0)
            val repaymentDetails = inputRepaymentBox.R8[Coll[Long]]
            val repaymentGoal = repaymentDetails.get(1)
            val lenderPk = inputRepaymentBox.R7[Coll[Byte]]

            val lenderRepaidBoxFunded = {
                allOf(Coll(
                    // ensure that this is the borrowers wallet
                    lenderRepaidBox.propositionBytes == lenderPk.get,
                    lenderRepaidBox.value >= fundingGoal
                ))
            }

            sigmaProp(lenderRepaidBoxFunded && repaymentBoxVerification && serviceBoxVerification)
        } else {
            // Defaulted
            val repaymentDetails = SELF.R8[Coll[Long]]
            val lenderPk = SELF.R7[Coll[Byte]]
            if (repaymentDetails.get(3) < HEIGHT) {
                // can be spent by lender.
                // all funds go back to lender
                val lenderRepaidBox = OUTPUTS(1)
                val lenderRepaidBoxFunded = {
                    allOf(Coll(
                        // ensure that this is the borrowers wallet
                        lenderRepaidBox.propositionBytes == lenderPk.get,
                        lenderRepaidBox.value >= SELF.value - _MinFee
                    ))
                }

                sigmaProp(lenderRepaidBoxFunded && repaymentBoxVerification && serviceBoxVerification)
            } else {
                sigmaProp(false)
            }
        }
    }
}