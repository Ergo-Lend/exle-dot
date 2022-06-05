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
    // Status           : 1st Draft Done

    // ===== Contract Hard-Coded Constants ===== //
    // val _MinFee:                         Long
    // val _OwnerPk:                        Coll[Byte]
    // val _SLTLendBoxHash:                 Digest32
    // val _SLTRepaymentBoxHash:            Digest32

    // ===== Contract Conditions ===== //
    // 1. Mutating Service Box          - Mutate Service box with new logic
    // 2. Lend Initiation               - Initialized/create a loan box
    // 3. Lend to Repayment             - Lend box succeeds in funding, and is converted to Repayment box
    // 4. Repayment Absorption          - Absorbing repayment box (may be defaulted or completed)
    //      a. Defaulted | Zero Interest        - As stated
    //      b. Completed                        - Repayment Success
    // 5. Lend Box Absorption           - Absorbing a lend box due to inactivity

    // ===== Service Check ===== //
    def serviceCheck(inputBox: Box, outputBox: Box): Boolean = {
        allOf(Coll(
            outputBox.propositionBytes      == inputBox.propositionBytes,
            outputBox.tokens(0).id          == inputBox.tokens(0).id,
            outputBox.tokens(1).id          == inputBox.tokens(1).id,
            outputBox.tokens(2).id          == inputBox.tokens(2).id,
            outputBox.value                 == inputBox.value,
            outputBox.R4[Coll[Long]]        == inputBox.R4[Coll[Long]],
            outputBox.R5[Coll[Coll[Byte]]]  == inputBox.R5[Coll[Coll[Byte]]],
            outputBox.R6[Coll[Byte]]        == inputBox.R6[Coll[Byte]],
            outputBox.R7[Coll[Byte]]        == inputBox.R7[Coll[Byte]],
            outputBox.R8[Coll[Long]]        == inputBox.R8[Coll[Long]]
        ))
    }

    // ===== Contract Constants ===== //
    // ##### Boxes ##### //
    val _inputServiceBox: Box                   = INPUTS(0)
    val _outputServiceBox: Box                  = OUTPUTS(0)

    // ##### Values ##### //
    val _serviceCreationInfo: Coll[Long]        = SELF.R4[Coll[Long]]
    val _serviceBoxInfo: Coll[Coll[Byte]]       = SELF.R5[Coll[Coll[Byte]]]
    val _serviceOwnerPubKey: Coll[Byte]         = SELF.R7[Coll[Byte]]
    val _serviceProfitSharingInfo: Coll[Long]   = SELF.R8[Coll[Long]]

    // ##### Conditions ##### //
    val _serviceFullCheck: Boolean              = serviceCheck(_inputServiceBox, _outputServiceBox)

    val _lendBoxTokensUnchanged: Boolean        = _inputServiceBox.tokens(1)._2 == _outputServiceBox.tokens(1)._2
    val _lendBoxTokenAbsorbed: Boolean          = _inputServiceBox.tokens(1)._2 + 1 == _outputServiceBox.tokens(1)._2
    val _lendBoxTokenDistribution: Boolean      = _inputServiceBox.tokens(1)._2 - 1 == _outputServiceBox.tokens(1)._2

    val _repaymentBoxTokensUnchanged: Boolean   = _inputServiceBox.tokens(2)._2 == _outputServiceBox.tokens(2)._2
    val _repaymentBoxTokenAbsorbed: Boolean     = _inputServiceBox.tokens(2)._2 + 1 == _outputServiceBox.tokens(2)._2
    val _repaymentBoxTokenDistribution: Boolean = _inputServiceBox.tokens(2)._2 - 1 == _outputServiceBox.tokens(2)._2

    // ##### Trigger Actions ##### //
    val _mutateServiceBox: Boolean              = {
        allOf(Coll(
            _lendBoxTokensUnchanged,
            _repaymentBoxTokensUnchanged
        ))
    }
    val _SLTLoanCreation: Boolean               = {
        allOf(Coll(
            _lendBoxTokenDistribution,
            _repaymentBoxTokensUnchanged
        ))
    }
    val _SLTLendToRepaymentConversion: Boolean  = {
        allOf(Coll(
            _lendBoxTokenAbsorbed,
            _repaymentBoxTokenDistribution
        ))
    }
    val _SLTRepaymentCompletion: Boolean        = {
        allOf(Coll(
            _lendBoxTokensUnchanged,
            _repaymentBoxTokenAbsorbed
        ))
    }
    val _SLTLoanAbsorption: Boolean             = {
        allOf(Coll(
            _lendBoxTokenAbsorbed,
            _repaymentBoxTokensUnchanged
        ))
    }

    // ===== Mutating service box ===== //
    // Description  : Mutate the box when there is no interaction with other boxes
    // Input Boxes  : 0 -> SELF
    // Output Boxes : 0 -> OutputSLTServiceBox, 1 -> MiningFee
    if (_mutateServiceBox)
    {
        _OwnerPk
    }
    else
    {
        // ===== Lend Initiation ===== //
        // Description  : Create a Loan
        // Input Boxes  : 0 -> SELF, 1 -> SLTCreateLendBoxPaymentBox
        // Output Boxes : 0 -> OutputSLTServiceBox, 1 -> ServiceFeeBox, 2 -> SLTLendBox, 3 -> MiningFee
        if (_SLTLoanCreation)
        {
            // ##### Boxes ##### //
            val serviceFeeBox: Box      = OUTPUTS(1)
            val sltLendBox: Box         = OUTPUTS(2)
        }
        else
        {
            // ===== Lend to Repayment (Lend Success) ===== //
            // Description  : Lend Box is fully funded, therefore we convert it to
            //              repayment box
            // Input Boxes  : 0 -> SELF, 1 -> SLTLendBox
            // Output Boxes : 0 -> OutputSLTServiceBox, 1 -> SLTRepaymentBox, 2 -> BorrowerLoanedFunds, 3 -> MiningFee
            if (_SLTLendToRepaymentConversion)
            {
                // ##### Boxes ##### //
                val sltLendBox: Box         = INPUTS(1)
                val sltRepaymentBox: Box    = OUTPUTS(1)

                val lendBoxCheck: Boolean       = {
                    allOf(Coll(
                        sltLendBox.tokens(0)._1     == SELF.tokens(1)._1,
                        blake2b256(sltLendBox.propositionBytes) == _SLTLendBoxHash
                    ))
                }

                val repaymentBoxCheck: Boolean  = {
                    allOf(Coll(
                        blake2b256(sltRepaymentBox.propositionBytes) == _SLTRepaymentBoxHash,
                        sltRepaymentBox.tokens(0)._1    == SELF.tokens(2)._1,
                        sltRepaymentBox.tokens(0)._2    == 1
                    ))
                }

                sigmaProp(allOf(Coll(
                    lendBoxCheck,
                    repaymentBoxCheck,
                    _serviceFullCheck
                )))
            }
            else
            {
                // ===== Repayment Absorption ===== //
                // Description  : Repayment Completed in one way or another (defaulted and what not),
                //              therefore we try to absorb the box
                // Input Boxes  : 0 -> SELF, 1 -> SLTRepaymentBox
                if (_SLTRepaymentCompletion)
                {
                    // ##### Contract Constants ##### //
                    // ##### Boxes ##### //
                    val sltRepaymentBox: Box    = INPUTS(1)

                    // ##### Values ##### //
                    val repaymentDetailsRegister: Coll[Long]    = sltRepaymentBox.R9[Coll[Long]]
                    val fundingInfoRegister: Coll[Long]         = sltRepaymentBox.R4[Coll[Long]]

                    val loanInterestRate: Long                  = fundingInfoRegister.get(2)
                    val repaymentAmount: Long                   = repaymentDetailsRegister.get(1)
                    val repaymentInterestAmount: Long           = repaymentDetailsRegister.get(2)
                    val repaymentHeightGoal: Long               = repaymentDetailsRegister.get(3)
                    val profitSharingPercentage: Long           = SELF.R8[Coll[Long]].get(0)
                    val profitSharingAmount: Long               = (repaymentInterestAmount * profitSharingPercentage) / 1000

                    // ===== Defaulted | Zero Interest Loans ===== //
                    // Description  : For conditions like, Defaulted, zero interest loan. We are not taking
                    //              profits. Therefore we run it differently as compared to fully funded
                    // Output Boxes : 0 -> OutputSLTServiceBox, 1 -> LenderRepaidFundBox, 2 -> MiningFee
                    val interestRateZero: Boolean       = loanInterestRate == 0
                    val defaulted: Boolean              = {
                        allOf(Coll(
                            repaymentHeightGoal < HEIGHT,
                            sltRepaymentBox.value < repaymentAmount
                        ))
                    }

                    if (interestRateZero || defaulted)
                    {
                        sigmaProp(_serviceFullCheck)
                    }
                    else
                    {
                        // ==== Successful Repayment ===== //
                        // Description  : The repayment is successful and we cant ake profit as
                        //              interest rate is not 0
                        // Output Boxes : 0 -> OutputSLTServiceBox, 1 -> ProfitSharingBox, 2 -> LenderRepaidFundBox, 3 -> MiningFee
                        // ##### Boxes ##### //
                        val profitSharingBox: Box       = OUTPUTS(1)

                        val profitSharingCheck: Boolean         = {
                            allOf(Coll(
                                profitSharingBox.propositionBytes   == _serviceOwnerPubKey.get,
                                profitSharingBox.tokens(0)._2       == profitSharingAmount
                            ))
                        }

                        sigmaProp(allOf(Coll(
                            profitSharingCheck,
                            _serviceFullCheck
                        )))
                    }
                }
                else
                {
                    // ===== Refund Lend Box ===== //
                    // Description  : LendBox is not funded, therefore we absorb the lendbox
                    // Input Boxes  : 0 -> SELF, 1 -> SLTLendBox
                    // Output Boxes : 0 -> OutputSLTServiceBox, 1 -> MiningFee
                    if (_SLTLoanAbsorption)
                    {
                        sigmaProp(allOf(Coll(
                            _serviceFullCheck,
                            _SLTLoanAbsorption
                        )))
                    }
                    else
                    {
                        sigmaProp(false)
                    }
                }
            }
        }
    }
}
