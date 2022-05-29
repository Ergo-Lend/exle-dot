{
    // ===== Contract Info ===== //
    // Name             : SLE [Single Lender Ergs] Service Box Guard Script
    // Description      : A single lender service box manages the boxes that are run throughout
    //                    the SLE system. This will ensure that lend and repayment boxes receives
    //                    their identification tokens. It ensures that the Exle DAO is paid through
    //                    service fees and interest cut.
    // Type             : Guard Script
    // Author           : Kii
    // Last Modified    : May 8th 2022
    // Version          : v 1.0
    // Status           : Completed

    // ===== Contract Hard-Coded Constants ===== //
    // val _MinFee:                     Long
    // val _OwnerPk:                    Coll[Byte]
    // val _LendBoxHash:                Digest32
    // val _RepaymentBoxHash:           Digest32

    // ===== Contract Conditions ===== //
    // 1. Fund Lend         - Fund the lend box when it is still fundable


    // Service Checks
    // - OwnerPub key, ProfitSharingPercentage
    // - propositionBytes, lendToken id, nft id, repaymentToken id, value

    val serviceCheck = allOf(Coll(
        OUTPUTS(0).propositionBytes == SELF.propositionBytes,
        OUTPUTS(0).tokens(0)._1 == SELF.tokens(0)._1,
        OUTPUTS(0).tokens(1)._1 == SELF.tokens(1)._1,
        OUTPUTS(0).tokens(2)._1 == SELF.tokens(2)._1,
        OUTPUTS(0).value == SELF.value
    ))

    // Mutating a box
    // --------------
    // When the tokens in both service box in Input and Output is the
    // same, we can mutate it.
    if (OUTPUTS(0).tokens(1)._2 == SELF.tokens(1)._2 &&
        OUTPUTS(0).tokens(2)._2 == SELF.tokens(2)._2) {

        _OwnerPk

    } else {

        val spendingServiceBoxCreationInfo = SELF.R4[Coll[Long]].get
        val spendingServiceBoxServiceInfo = SELF.R5[Coll[Coll[Byte]]]
        val spendingServiceBoxBoxInfo = SELF.R6[Coll[Byte]]
        val spendingServiceBoxOwnerPubKey = SELF.R7[Coll[Byte]]
        val spendingServiceBoxProfitSharing = SELF.R8[Coll[Long]]

        val outputServiceBoxCreationInfo = OUTPUTS(0).R4[Coll[Long]].get
        val outputServiceBoxServiceInfo = OUTPUTS(0).R5[Coll[Coll[Byte]]]
        val outputServiceBoxBoxInfo = OUTPUTS(0).R6[Coll[Byte]]
        val outputServiceBoxOwnerPubKey = OUTPUTS(0).R7[Coll[Byte]]
        val outputServiceBoxProfitSharing = OUTPUTS(0).R8[Coll[Long]]

        val serviceRegisterCheck = allOf(Coll(
            spendingServiceBoxCreationInfo == outputServiceBoxCreationInfo,
            spendingServiceBoxServiceInfo == outputServiceBoxServiceInfo,
            spendingServiceBoxBoxInfo == outputServiceBoxBoxInfo,
            spendingServiceBoxOwnerPubKey == outputServiceBoxOwnerPubKey,
            spendingServiceBoxProfitSharing == outputServiceBoxProfitSharing,
        ))

        val serviceFullCheck = allOf(Coll(
            serviceCheck,
            serviceRegisterCheck
        ))

        // Lend Initiation
        // Service, Proxy -> Service, LendBox
        if ((OUTPUTS(0).tokens(1)._2 == SELF.tokens(1)._2 - 1) &&
            OUTPUTS(0).tokens(2)._2 == SELF.tokens(2)._2) {
            val serviceFee = spendingServiceBoxProfitSharing.get(1)
            val serviceFeeBox = OUTPUTS(2)

            val lendBox = OUTPUTS(1)
            val fundingInfoRegister = lendBox.R4[Coll[Long]]

            val fundingGoal = fundingInfoRegister.get(0)
            val deadlineHeight = fundingInfoRegister.get(1)
            val interestRate = fundingInfoRegister.get(2)
            val repaymentHeightLength = fundingInfoRegister.get(3)

            val lendBoxCheck = {
                allOf(Coll(
                    fundingGoal > 0,
                    deadlineHeight - HEIGHT > 0,
                    interestRate >= 0,
                    repaymentHeightLength > 0
                ))
            }

            val isLendInitiationServiceCheck = {
                allOf(Coll(
                    blake2b256(lendBox.propositionBytes) == _LendBoxHash,
                    serviceFullCheck,
                    serviceFeeBox.value >= serviceFee,
                    serviceFeeBox.propositionBytes == spendingServiceBoxOwnerPubKey.get
                ))
            }

            sigmaProp(isLendInitiationServiceCheck && lendBoxCheck)
        } else {
            // Lend Success
            // Service, LendBox -> Service, RepaymentBox, BorrowerLoanedFunds
            if ((OUTPUTS(0).tokens(1)._2 == SELF.tokens(1)._2 + 1) &&
                OUTPUTS(0).tokens(2)._2 == SELF.tokens(2)._2 - 1) {

                sigmaProp(allOf(Coll(
                    blake2b256(OUTPUTS(1).propositionBytes) == _RepaymentBoxHash,
                    serviceFullCheck
                )))
            } else {
                // ** Repayment Success **
                // Service, Repayment -> Service, ProfitSharing, LenderRepaidFunds
                if ((OUTPUTS(0).tokens(1)._2 == SELF.tokens(1)._2) &&
                    OUTPUTS(0).tokens(2)._2 == SELF.tokens(2)._2 + 1) {
                    // Profit Sharing
                    val profitSharingBox = OUTPUTS(2)
                    val lendersBox = OUTPUTS(1)

                    val profitSharingBoxOwnerValidation =
                        profitSharingBox.propositionBytes == spendingServiceBoxOwnerPubKey.get
                    val repaymentDetails = INPUTS(1).R8[Coll[Long]]
                    val repaymentAmount = repaymentDetails.get(1)
                    val repaymentInterestAmount = repaymentDetails.get(2)

                    // Interest rate above 1%, ErgoLend takes Profit.
                    // If Interest rate is at 0%, ErgoLend does Charity too
                    val fundingInfo = INPUTS(1).R4[Coll[Long]]
                    val interestRate = fundingInfo.get(2)

                    val defaulted = repaymentDetails.get(3) < HEIGHT && INPUTS(1).value < repaymentDetails.get(1)

                    /**
                      *   Defaulted || Zero Interest Loans
                      *
                      *   Defaulted meaning, the repayments are not fully
                      *   funded, the deadline has passed, and they decide
                      *   to take their loss.
                      *
                      *   For both Defaulted and Zero Interest Loans,
                      *   ErgoLend will not take a cut.
                      *
                      *   Note: We are still taking profit if the loan
                      *     gets repaid after defaulting.
                      **/
                    if (interestRate == 0 || defaulted) {
                        // There will only be servicebox and lender box (+ miner's fee)
                        // not putting count of boxes for potential credit check
                        sigmaProp(serviceFullCheck)
                    } else {
                        // Take profit
                        val profitSharingAmount = (repaymentInterestAmount * spendingServiceBoxProfitSharing.get(0)) /1000
                        val profitSharingAmountValidation = profitSharingBox.value >= profitSharingAmount

                        // Gives a margin error of _MinFee
                        val repaymentLendBoxAmount = repaymentAmount - profitSharingAmount - (2 * _MinFee)
                        val repaymentBoxRepaid = lendersBox.value >= repaymentLendBoxAmount

                        if (profitSharingAmount > _MinFee) {
                            sigmaProp(allOf(Coll(
                                profitSharingAmountValidation,
                                serviceFullCheck,
                                profitSharingBoxOwnerValidation,
                                repaymentBoxRepaid
                            )))
                        } else {
                            sigmaProp(serviceFullCheck && repaymentBoxRepaid)
                        }
                    }

                } else {
                    // Refund Lend Box
                    if ((OUTPUTS(0).tokens(1)._2 == SELF.tokens(1)._2 + 1) &&
                        OUTPUTS(0).tokens(2)._2 == SELF.tokens(2)._2) {
                        sigmaProp(serviceFullCheck)
                    } else {
                        sigmaProp(false)
                    }
                }
            }
        }
    }
}