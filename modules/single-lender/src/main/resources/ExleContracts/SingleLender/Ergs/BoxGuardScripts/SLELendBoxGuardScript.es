{
    // ===== Contract Info ===== //
    // Name             : SLE [Single Lender Ergs] Lend Box Guard Script
    // Description      : A single lender lend box allows interested lenders to participate
    //                    in the activity of lending to a borrower. The box ensures that
    //                    all value within the box is funded to the borrower and borrower
    //                    only. It also ensures that a repayment box is created upon successful
    //                    lending.
    // Type             : Guard Script
    // Author           : Kii
    // Last Modified    : May 8th 2022
    // Version          : v 1.0
    // Status           : Completed

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

    val lendBoxFundingInfo = SELF.R4[Coll[Long]]
    val lendBoxProjectDetails = SELF.R5[Coll[Coll[Byte]]]
    val lendBoxBorrowerPk = SELF.R6[Coll[Byte]]

    val fundingGoal = lendBoxFundingInfo.get(0)
    val deadlineHeight = lendBoxFundingInfo.get(1)
    val interestRate = lendBoxFundingInfo.get(2)
    val repaymentHeightLength = lendBoxFundingInfo.get(3)

    val selfValue = SELF.value

    val lendBoxVerification = allOf(Coll(
        SELF.tokens(0)._1 == _SLELendTokenId,
        SELF.tokens(0)._2 == 1))

    val lendBoxLenderPk = SELF.R7[Coll[Byte]]
    val isLenderEmpty = !lendBoxLenderPk.isDefined
    val lendBoxSuccessfullyFunded = selfValue == (fundingGoal + (_MinFee * 2))
    if (lendBoxSuccessfullyFunded && !isLenderEmpty) {

        val serviceBoxCheck = INPUTS(0).tokens(0)._1 == _SLEServiceNFTId

        // Scenario 3: ** FUNDED **
        // Checks:
        // - RepaymentBox having the same details as LendBox
        // - RepaymentBox has new detail about repayment info.
        // - BorrowerFund: Gets full value from funds
        val repaymentBox = OUTPUTS(1)
        val repaymentBoxToken = repaymentBox.tokens(0)._1
        val repaymentBoxTokenCount = repaymentBox.tokens(0)._2
        val repaymentBoxFundingInfo = repaymentBox.R4[Coll[Long]]
        val repaymentBoxProjectDetails = repaymentBox.R5[Coll[Coll[Byte]]]
        val repaymentBoxBorrowerPk = repaymentBox.R6[Coll[Byte]]
        val repaymentBoxLenderPk = repaymentBox.R7[Coll[Byte]]

        // compare fund details
        val fundDetailReplication = allOf(Coll(
            repaymentBoxToken == _SLERepaymentTokenId,
            repaymentBoxTokenCount == 1,
            repaymentBoxFundingInfo.get == lendBoxFundingInfo.get,
            repaymentBoxProjectDetails.get == lendBoxProjectDetails.get,
            repaymentBoxBorrowerPk.get == lendBoxBorrowerPk.get,
            repaymentBoxLenderPk.get == lendBoxLenderPk.get
        ))

        // 1. funded height, repaymentFundedValue
        val repaymentDetails = repaymentBox.R8[Coll[Long]]
        val repaymentBoxRepaymentAmount = repaymentDetails.get(1)
        val repaymentBoxInterestRate = repaymentDetails.get(2)

        val totalInterestAmount = (fundingGoal * interestRate/1000)

        val repaymentDetailsCheck = allOf(Coll(
            repaymentBoxInterestRate == totalInterestAmount,
            repaymentBoxRepaymentAmount == (fundingGoal + totalInterestAmount)
        ))

        // the 3rd should be the borrowers address. That's where the fund will go
        val borrowerFundedBox = OUTPUTS(2)
        val borrowerFundedAmount = fundingGoal

        val borrowerBoxFunded = allOf(Coll(
            // ensure that this is the borrowers wallet
            borrowerFundedBox.propositionBytes == lendBoxBorrowerPk.get,
            borrowerFundedBox.value == borrowerFundedAmount
        ))

        sigmaProp(
            serviceBoxCheck &&
            lendBoxVerification &&
            fundDetailReplication &&
            borrowerBoxFunded &&
            repaymentDetailsCheck)
        // ** Funded done **
    } else {

        // ** NOT FUNDED **
        // we first check if it passed the deadline
        // If it passes deadline, it's consumable. Because it's a minBox
        // there's no refund.
        val deadlinePassed = HEIGHT > deadlineHeight
        if (deadlinePassed) {
            val serviceBoxCheck = INPUTS(0).tokens(0)._1 == _SLEServiceNFTId

            // Scenario 4: ** DEADLINE PASSED: REFUND **
            // Inputs: ServiceBox, LendBox
            // Outputs: ServiceBox
            //
            // Checks:
            // - ServiceBox has its own checks. Therefore not needed here

            sigmaProp(allOf(Coll(serviceBoxCheck)))

        } else {

            // Scenario 4: ** Mistakenly funded during creation **
            //   Refund to Borrower
            val serviceBoxCheck = INPUTS(0).tokens(0)._1 == _SLEServiceNFTId

            if (serviceBoxCheck && isLenderEmpty) {
                val outputRefundBox = OUTPUTS(1)
                val lendRefundBox = INPUTS(1)
                val valueRefunded = outputRefundBox.value == lendRefundBox.value - _MinFee
                val lendRefundBoxIsBorrower = outputRefundBox.propositionBytes == lendBoxBorrowerPk.get

                sigmaProp(
                    serviceBoxCheck &&
                    valueRefunded &&
                    lendRefundBoxIsBorrower
                )
            } else {
                // Scenario 2: ** LENDING ACTIVE: LEND **

                val outputLendBox = OUTPUTS(0)
                val lendProxyContract = INPUTS(1)

                val outputLendBoxFundingInfo = outputLendBox.R4[Coll[Long]]
                val outputLendBoxProjectDetails = outputLendBox.R5[Coll[Coll[Byte]]]
                val outputLendBoxBorrowerPk = outputLendBox.R6[Coll[Byte]]

                val lendBoxDetailReplicationAndInstantiation = allOf(Coll(
                    lendBoxFundingInfo == outputLendBoxFundingInfo,
                    lendBoxProjectDetails == outputLendBoxProjectDetails,
                    lendBoxBorrowerPk == outputLendBoxBorrowerPk
                ))

                // Check lenderRegister is defined
                val outputBoxLenderRegister = outputLendBox.R7[Coll[Byte]]
                val lenderRegisterDefined = outputBoxLenderRegister.isDefined

                // Note: In a single lender lend box, only one lender can lend
                //      therefore if the lender funds the box, they have to fund
                //      the full amount
                val newFundedValue = fundingGoal + (_MinFee * 2) + _MinBoxAmount - SELF.value

                val valueTransferred = allOf(Coll(
                    newFundedValue == outputLendBox.value
                ))

                sigmaProp(
                    allOf(Coll(
                        lendBoxVerification,
                        lendBoxDetailReplicationAndInstantiation,
                        valueTransferred,
                        lenderRegisterDefined
                    ))
                )
                // ** LENDED **
            }
        }
    }
}