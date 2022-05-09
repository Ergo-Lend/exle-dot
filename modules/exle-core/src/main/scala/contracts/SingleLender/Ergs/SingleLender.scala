package contracts.SingleLender.Ergs

object SingleLender {
  /**
   * Funding Box V1
   * One Lender only
   *
   * For this script, we have to consider a few scenarios.
   * 1. Initiated
   * 2. Funded
   * 3. Not Funded passed deadline
   *
   * We will first split the problem out into 2, funded or not funded.
   * When a lending box is funded, it doesn't matter if it passed the deadline,
   * it should move to the next phase.
   *
   * If its not funded passed deadline, no funds can be added. Only refunded to lenders
   *
   * If its still funding, it can accept more funding and have an increase in value.
   *
   *
   * Scenario 1: Lend Initiated
   *
   * Funds from proxy contracts are collected and lend box is initiated
   * Checks:
   * - LendBox containing LendToken
   * - Funding details are correct
   *
   * Input:
   * - Service Box
   * - Proxy Contract funds
   *
   * Output:
   * - Service Box
   * - LendBox
   *
   * Scenario 2: Lend Funding
   *
   * Funds from proxy contracts for funding are collected and merged with
   * lend box. In a singleLender lendBox, the funds have to be equal to the funding goal
   *
   * Checks:
   * - Funds equal to funding goal
   *
   * Input:
   * - LendBox
   * - Proxy Contract
   *
   * Output:
   * - LendBox
   *
   * Scenario 3: Lend Funded
   *
   * LendBox is switched to RepaymentBox. All details are transferred over, with
   * repayment details included. Checking for ServiceBox is not required
   *
   * Checks:
   * - LendBox details == RepaymentBox details
   * - RepaymentBox details correct
   *
   * Input:
   * - ServiceBox
   * - LendBox
   *
   * Output:
   * - ServiceBox
   * - RepaymentBox
   *
   * Scenario 4: Lend Refund
   *
   * LendBox is refunded to borrower. LendBox is consume, and the value gets returned
   * to the borrower
   *
   * Checks:
   * - Box to Borrower == borrowers Proposition Bytes
   *
   * Input:
   * - ServiceBox
   * - LendBox
   *
   * Output:
   * - ServiceBox
   * - Refund to Borrower
   *
   *
   * # Note: We are not checking lender Pk when it is funding.
   *
   * Variables:
   * - minFee
   * - repaymentBoxMinAmount
   * - serviceNFT
   * - serviceLendToken
   * - serviceRepaymentToken
   * - emptyLenderPk
   */
  lazy val singleLenderLendBoxScript: String =
    s"""{
       |  val lendBoxFundingInfo = SELF.R4[Coll[Long]]
       |  val lendBoxProjectDetails = SELF.R5[Coll[Coll[Byte]]]
       |  val lendBoxBorrowerPk = SELF.R6[Coll[Byte]]
       |
       |  val fundingGoal = lendBoxFundingInfo.get(0)
       |  val deadlineHeight = lendBoxFundingInfo.get(1)
       |  val interestRate = lendBoxFundingInfo.get(2)
       |  val repaymentHeightLength = lendBoxFundingInfo.get(3)
       |
       |  val selfValue = SELF.value
       |
       |  val lendBoxVerification = allOf(Coll(
       |    SELF.core.SingleLender.Ergs.tokens(0)._1 == serviceLendToken,
       |    SELF.core.SingleLender.Ergs.tokens(0)._2 == 1))
       |
       |  val lendBoxLenderPk = SELF.R7[Coll[Byte]]
       |  val isLenderEmpty = !lendBoxLenderPk.isDefined
       |  val lendBoxSuccessfullyFunded = selfValue == (fundingGoal + (minFee * 2))
       |  if (lendBoxSuccessfullyFunded && !isLenderEmpty) {
       |
       |    val serviceBoxCheck = INPUTS(0).core.SingleLender.Ergs.tokens(0)._1 == serviceNFT
       |
       |    // Scenario 3: ** FUNDED **
       |    // Checks:
       |    // - RepaymentBox having the same details as LendBox
       |    // - RepaymentBox has new detail about repayment info.
       |    // - BorrowerFund: Gets full value from funds
       |
       |
       |    val repaymentBox = OUTPUTS(1)
       |    val repaymentBoxToken = repaymentBox.core.SingleLender.Ergs.tokens(0)._1
       |    val repaymentBoxTokenCount = repaymentBox.core.SingleLender.Ergs.tokens(0)._2
       |    val repaymentBoxFundingInfo = repaymentBox.R4[Coll[Long]]
       |    val repaymentBoxProjectDetails = repaymentBox.R5[Coll[Coll[Byte]]]
       |    val repaymentBoxBorrowerPk = repaymentBox.R6[Coll[Byte]]
       |    val repaymentBoxLenderPk = repaymentBox.R7[Coll[Byte]]
       |
       |    // compare fund details
       |    val fundDetailReplication = allOf(Coll(
       |      repaymentBoxToken == serviceRepaymentToken,
       |      repaymentBoxTokenCount == 1,
       |      repaymentBoxFundingInfo.get == lendBoxFundingInfo.get,
       |      repaymentBoxProjectDetails.get == lendBoxProjectDetails.get,
       |      repaymentBoxBorrowerPk.get == lendBoxBorrowerPk.get,
       |      repaymentBoxLenderPk.get == lendBoxLenderPk.get
       |    ))
       |
       |
       |    // 1. funded height, repaymentFundedValue
       |    val repaymentDetails = repaymentBox.R8[Coll[Long]]
       |    val repaymentBoxRepaymentAmount = repaymentDetails.get(1)
       |    val repaymentBoxInterestRate = repaymentDetails.get(2)
       |
       |    val totalInterestAmount = (fundingGoal * interestRate/1000)
       |
       |    val repaymentDetailsCheck = allOf(Coll(
       |      repaymentBoxInterestRate == totalInterestAmount,
       |      repaymentBoxRepaymentAmount == (fundingGoal + totalInterestAmount)
       |    ))
       |
       |
       |    // the 3rd should be the borrowers address. That's where the fund will go
       |    val borrowerFundedBox = OUTPUTS(2)
       |    val borrowerFundedAmount = fundingGoal
       |
       |    val borrowerBoxFunded = allOf(Coll(
       |        // ensure that this is the borrowers wallet
       |        borrowerFundedBox.propositionBytes == lendBoxBorrowerPk.get,
       |        borrowerFundedBox.value == borrowerFundedAmount
       |      ))
       |
       |    sigmaProp(
       |      serviceBoxCheck &&
       |      lendBoxVerification &&
       |      fundDetailReplication &&
       |      borrowerBoxFunded &&
       |      repaymentDetailsCheck)
       |
       |    // ** Funded done **
       |
       |  } else {
       |
       |    // ** NOT FUNDED **
       |    // we first check if it passed the deadline
       |    // If it passes deadline, it's consumable. Because it's a minBox
       |    // there's no refund.
       |    val deadlinePassed = HEIGHT > deadlineHeight
       |    if (deadlinePassed) {
       |      val serviceBoxCheck = INPUTS(0).core.SingleLender.Ergs.tokens(0)._1 == serviceNFT
       |
       |      // Scenario 4: ** DEADLINE PASSED: REFUND **
       |      // Inputs: ServiceBox, LendBox
       |      // Outputs: ServiceBox
       |      //
       |      // Checks:
       |      // - ServiceBox has its own checks. Therefore not needed here
       |
       |      sigmaProp(allOf(Coll(serviceBoxCheck)))
       |
       |    } else {
       |
       |      // Scenario 4: ** Mistakenly funded during creation **
       |      //   Refund to Borrower
       |      val serviceBoxCheck = INPUTS(0).core.SingleLender.Ergs.tokens(0)._1 == serviceNFT
       |
       |      if (serviceBoxCheck && isLenderEmpty) {
       |        val outputRefundBox = OUTPUTS(1)
       |        val lendRefundBox = INPUTS(1)
       |        val valueRefunded = outputRefundBox.value == lendRefundBox.value - minFee
       |        val lendRefundBoxIsBorrower = outputRefundBox.propositionBytes == lendBoxBorrowerPk.get
       |
       |        sigmaProp(
       |          serviceBoxCheck &&
       |          valueRefunded &&
       |          lendRefundBoxIsBorrower
       |        )
       |      } else {
       |        // Scenario 2: ** LENDING ACTIVE: LEND **
       |
       |        val outputLendBox = OUTPUTS(0)
       |        val lendProxyContract = INPUTS(1)
       |
       |        val outputLendBoxFundingInfo = outputLendBox.R4[Coll[Long]]
       |        val outputLendBoxProjectDetails = outputLendBox.R5[Coll[Coll[Byte]]]
       |        val outputLendBoxBorrowerPk = outputLendBox.R6[Coll[Byte]]
       |
       |        val lendBoxDetailReplicationAndInstantiation = allOf(Coll(
       |          lendBoxFundingInfo == outputLendBoxFundingInfo,
       |          lendBoxProjectDetails == outputLendBoxProjectDetails,
       |          lendBoxBorrowerPk == outputLendBoxBorrowerPk
       |        ))
       |
       |        // Check lenderRegister is defined
       |        val outputBoxLenderRegister = outputLendBox.R7[Coll[Byte]]
       |        val lenderRegisterDefined = outputBoxLenderRegister.isDefined
       |
       |        // Note: In a single lender lend box, only one lender can lend
       |        //      therefore if the lender funds the box, they have to fund
       |        //      the full amount
       |        val newFundedValue = fundingGoal + (minFee * 2) + minBoxAmount - SELF.value
       |
       |        val valueTransferred = allOf(Coll(
       |          newFundedValue == outputLendBox.value
       |        ))
       |
       |        sigmaProp(
       |          allOf(Coll(
       |            lendBoxVerification,
       |            lendBoxDetailReplicationAndInstantiation,
       |            valueTransferred,
       |            lenderRegisterDefined
       |          ))
       |        )
       |
       |        // ** LENDED **
       |      }
       |    }
       |  }
       |}
       |""".stripMargin

  /**
   * SingleLenderRepaymentBox
   * Only One lender
   *
   * The repayment box can be funded in incremental value.
   * However, since there is only one lender, the output
   * for the repayment when it is completed is only back to 1 lender.
   *
   * Three Scenario:
   * 1. RepaymentBox Created
   * 2. RepaymentBox Funding
   * 3. RepaymentBox Fully Funded (to be spent)
   *
   *
   * Input Variables:
   * - minFee
   * - serviceRepaymentToken
   * - serviceLendToken
   * - serviceBoxNFT
   */
  lazy val singleLenderRepaymentBoxScript: String =
    s"""{
       |  val repaymentBoxVerification = SELF.core.SingleLender.Ergs.tokens(0)._1 == serviceRepaymentToken
       |  val serviceBox = INPUTS(0)
       |  val serviceBoxVerification = INPUTS(0).core.SingleLender.Ergs.tokens(0)._1 == serviceBoxNFT
       |
       |  // If 1st Output's propositionBytes is the same, then its a repayment box
       |  // INPUTS(0) and OUTPUTS(0) can be true during fund success
       |  val isRepaymentBox = INPUTS(0).propositionBytes == OUTPUTS(0).propositionBytes
       |  if (isRepaymentBox && !serviceBoxVerification) {
       |    // *** ADDING FUNDS ***
       |    // RepaymentBox, ProxyContract -> OutRepaymentBox
       |    // @variables minFee
       |
       |    val repaymentBoxInput = SELF
       |    val repaymentBoxOutput = OUTPUTS(0)
       |    val proxyBox = INPUTS(1)
       |
       |    val inputRepaymentBoxAccounting = repaymentBoxInput.R4[Coll[Long]]
       |    val inputRepaymentBoxDetails = repaymentBoxInput.R5[Coll[Coll[Byte]]]
       |    val inputRepaymentBoxBorrower = repaymentBoxInput.R6[Coll[Byte]]
       |    val inputRepaymentBoxLender = repaymentBoxInput.R7[Coll[Byte]]
       |    val inputRepaymentBoxRepaymentDetails = repaymentBoxInput.R8[Coll[Long]]
       |
       |    val outputRepaymentBoxAccounting = repaymentBoxOutput.R4[Coll[Long]]
       |    val outputRepaymentBoxDetails = repaymentBoxOutput.R5[Coll[Coll[Byte]]]
       |    val outputRepaymentBoxBorrower = repaymentBoxOutput.R6[Coll[Byte]]
       |    val outputRepaymentBoxLender = repaymentBoxOutput.R7[Coll[Byte]]
       |    val outputRepaymentBoxRepaymentDetails = repaymentBoxOutput.R8[Coll[Long]]
       |
       |    val repaymentBoxDetailCheck = {
       |      allOf(Coll(
       |        repaymentBoxVerification,
       |        outputRepaymentBoxAccounting == inputRepaymentBoxAccounting,
       |        outputRepaymentBoxDetails == inputRepaymentBoxDetails,
       |        outputRepaymentBoxBorrower == inputRepaymentBoxBorrower,
       |        outputRepaymentBoxLender == inputRepaymentBoxLender,
       |        outputRepaymentBoxRepaymentDetails == inputRepaymentBoxRepaymentDetails
       |      ))
       |    }
       |
       |    val repaymentNotFullyFunded = repaymentBoxInput.value < SELF.R8[Coll[Long]].get(1)
       |    val transferredValue = repaymentBoxInput.value + proxyBox.value - minFee
       |    val isValueTransferred = repaymentBoxOutput.value == transferredValue
       |    val repaymentGoal = inputRepaymentBoxRepaymentDetails.get(1)
       |
       |    /**
       |      * When overfunded
       |      *
       |      * We fill the repayment box and return the rest to the funder
       |      * ErgoLend's proxy contract ensures the funds get returned to
       |      * funder. This is because anyone can fund a repayment, and the
       |      * funds should return to its rightful owner.
       |      **/
       |    val overfunded = transferredValue > repaymentGoal
       |    if (overfunded) {
       |      val goaledRepaymentBox = repaymentBoxOutput.value == repaymentGoal + minFee
       |      val repaymentGoalReached = {
       |        allOf(Coll(
       |          repaymentBoxDetailCheck,
       |          repaymentNotFullyFunded,
       |          goaledRepaymentBox
       |        ))
       |      }
       |
       |      sigmaProp(repaymentGoalReached)
       |    } else {
       |      val repaymentFundingFulfilled = {
       |        allOf(Coll(
       |          isValueTransferred,
       |          repaymentBoxDetailCheck,
       |          repaymentNotFullyFunded
       |        ))
       |      }
       |
       |      sigmaProp(repaymentFundingFulfilled)
       |    }
       |
       |  } else {
       |
       |    // Scenario 3: Fully funded
       |    // Spend to return to lender
       |    // ServiceBox, RepaymentBox -> ServiceBox, ProfitSharing, LenderFundedBox
       |    // *** REPAYMENT FULFILLED ***
       |
       |    val selfRepaymentBox = SELF
       |    val selfFundingDetails = SELF.R4[Coll[Long]]
       |    val selfLendDetails = SELF.R5[Coll[Coll[Byte]]]
       |    val selfBorrowerPk = SELF.R6[Coll[Byte]]
       |    val selfLenderPk = SELF.R7[Coll[Byte]]
       |    val selfRepaymentDetails = SELF.R8[Coll[Long]]
       |
       |    val fundingSuccessful = selfRepaymentBox.value >= selfRepaymentDetails.get(1)
       |    if (fundingSuccessful) {
       |
       |      val inputRepaymentBox = SELF
       |      val lenderRepaidBox = OUTPUTS(1)
       |
       |      // compare lender's box and value
       |      val inputRepaymentAcc = inputRepaymentBox.R4[Coll[Long]]
       |      val fundingGoal = inputRepaymentAcc.get(0)
       |      val repaymentDetails = inputRepaymentBox.R8[Coll[Long]]
       |      val repaymentGoal = repaymentDetails.get(1)
       |      val lenderPk = inputRepaymentBox.R7[Coll[Byte]]
       |
       |      val lenderRepaidBoxFunded = {
       |        allOf(Coll(
       |          // ensure that this is the borrowers wallet
       |          lenderRepaidBox.propositionBytes == lenderPk.get,
       |          lenderRepaidBox.value >= fundingGoal
       |        ))
       |      }
       |
       |      sigmaProp(lenderRepaidBoxFunded && repaymentBoxVerification && serviceBoxVerification)
       |    } else {
       |      // Defaulted
       |      val repaymentDetails = SELF.R8[Coll[Long]]
       |      val lenderPk = SELF.R7[Coll[Byte]]
       |      if (repaymentDetails.get(3) < HEIGHT) {
       |        // can be spent by lender.
       |        // all funds go back to lender
       |        val lenderRepaidBox = OUTPUTS(1)
       |        val lenderRepaidBoxFunded = {
       |          allOf(Coll(
       |            // ensure that this is the borrowers wallet
       |            lenderRepaidBox.propositionBytes == lenderPk.get,
       |            lenderRepaidBox.value >= SELF.value - minFee
       |          ))
       |        }
       |        sigmaProp(lenderRepaidBoxFunded && repaymentBoxVerification && serviceBoxVerification)
       |      } else {
       |        sigmaProp(false)
       |      }
       |    }
       |  }
       |}
       |""".stripMargin

  /**
   * ServiceBox: SingleLender Contract
   *
   * Scenario 1 -> Mutation
   * Service Box information can be mutable for improvement
   * by Owner (ErgoLend)
   *
   * Scenario 2 -> Lend Initiation
   * Service Box provides a token to create a lendingBox
   * Checks:
   * Service Check
   * Lend token == 1 in lend box
   * Service Box lend token == -1 of total
   *
   * Scenario 3 -> Lend Funded
   * Service box switches LendToken to RepaymentToken
   * Box to Lender
   * // Checks:
   * // - Service box: Lend token + 1
   * // - Service box: Repayment token - 1
   * // - LendBox: Value >= funding goal
   * // - LendBox: Lend Token == 1 (was 1 before tx)
   * // - RepaymentBox: Repayment token == 1
   * //
   * //    Borrower Box
   * // - Value: >= lendingFund
   * // - PropositionBytes == Borrowers
   *
   * Scenario 4 -> Repayment Consumption
   * Service Box receives token from Repayment Box
   * Service Box uses PubKey and ProfitSharingPercentage to create
   * ProfitSharingBox to ErgoLend
   * // Checks:
   * // Total Output Box: 3 (ServiceBox, ProfitSharingBox, Lender's Box)
   * // Service box: Lend Token == same
   * // Service box: Repayment Token + 1
   * // Profit Sharing Box: PropositionBytes == profitSharingBoxOwnerPubKey
   * // RepaymentBox: Value >= repaymentValue
   *
   * Scenario 5 -> Refunds
   * Refunds only happen when lend box is not successful
   * In the refunds, the lendToken is return to the serviceBox
   *
   * Input variables:
   * - serviceNFT
   * - serviceLendToken
   * - serviceRepaymentToken
   * - ownerPk
   * - lendBoxHash
   * - repaymentBoxHash
   * - minFee
   */
  lazy val singleLenderLendServiceBoxScript: String =
    s"""{
       |  // Service Checks
       |  // - OwnerPub key, ProfitSharingPercentage
       |  // - propositionBytes, lendToken id, nft id, repaymentToken id, value
       |
       |  val serviceCheck = allOf(Coll(
       |      OUTPUTS(0).propositionBytes == SELF.propositionBytes,
       |      OUTPUTS(0).core.SingleLender.Ergs.tokens(0)._1 == SELF.core.SingleLender.Ergs.tokens(0)._1,
       |      OUTPUTS(0).core.SingleLender.Ergs.tokens(1)._1 == SELF.core.SingleLender.Ergs.tokens(1)._1,
       |      OUTPUTS(0).core.SingleLender.Ergs.tokens(2)._1 == SELF.core.SingleLender.Ergs.tokens(2)._1,
       |      OUTPUTS(0).value == SELF.value
       |    ))
       |
       |  // Mutating a box
       |  // --------------
       |  // When the core.SingleLender.Ergs.tokens in both service box in Input and Output is the
       |  // same, we can mutate it.
       |  if (OUTPUTS(0).core.SingleLender.Ergs.tokens(1)._2 == SELF.core.SingleLender.Ergs.tokens(1)._2 &&
       |    OUTPUTS(0).core.SingleLender.Ergs.tokens(2)._2 == SELF.core.SingleLender.Ergs.tokens(2)._2) {
       |      ownerPk
       |  } else {
       |    val spendingServiceBoxCreationInfo = SELF.R4[Coll[Long]].get
       |    val spendingServiceBoxServiceInfo = SELF.R5[Coll[Coll[Byte]]]
       |    val spendingServiceBoxBoxInfo = SELF.R6[Coll[Byte]]
       |    val spendingServiceBoxOwnerPubKey = SELF.R7[Coll[Byte]]
       |    val spendingServiceBoxProfitSharing = SELF.R8[Coll[Long]]
       |
       |    val outputServiceBoxCreationInfo = OUTPUTS(0).R4[Coll[Long]].get
       |    val outputServiceBoxServiceInfo = OUTPUTS(0).R5[Coll[Coll[Byte]]]
       |    val outputServiceBoxBoxInfo = OUTPUTS(0).R6[Coll[Byte]]
       |    val outputServiceBoxOwnerPubKey = OUTPUTS(0).R7[Coll[Byte]]
       |    val outputServiceBoxProfitSharing = OUTPUTS(0).R8[Coll[Long]]
       |
       |    val serviceRegisterCheck = allOf(Coll(
       |      spendingServiceBoxCreationInfo == outputServiceBoxCreationInfo,
       |      spendingServiceBoxServiceInfo == outputServiceBoxServiceInfo,
       |      spendingServiceBoxBoxInfo == outputServiceBoxBoxInfo,
       |      spendingServiceBoxOwnerPubKey == outputServiceBoxOwnerPubKey,
       |      spendingServiceBoxProfitSharing == outputServiceBoxProfitSharing,
       |    ))
       |
       |    val serviceFullCheck = allOf(Coll(
       |      serviceCheck,
       |      serviceRegisterCheck
       |    ))
       |
       |    // Lend Initiation
       |    // Service, Proxy -> Service, LendBox
       |    if ((OUTPUTS(0).core.SingleLender.Ergs.tokens(1)._2 == SELF.core.SingleLender.Ergs.tokens(1)._2 - 1) &&
       |      OUTPUTS(0).core.SingleLender.Ergs.tokens(2)._2 == SELF.core.SingleLender.Ergs.tokens(2)._2) {
       |      val serviceFee = spendingServiceBoxProfitSharing.get(1)
       |      val serviceFeeBox = OUTPUTS(2)
       |
       |      val lendBox = OUTPUTS(1)
       |      val fundingInfoRegister = lendBox.R4[Coll[Long]]
       |
       |      val fundingGoal = fundingInfoRegister.get(0)
       |      val deadlineHeight = fundingInfoRegister.get(1)
       |      val interestRate = fundingInfoRegister.get(2)
       |      val repaymentHeightLength = fundingInfoRegister.get(3)
       |
       |      val lendBoxCheck = {
       |        allOf(Coll(
       |          fundingGoal > 0,
       |          deadlineHeight - HEIGHT > 0,
       |          interestRate >= 0,
       |          repaymentHeightLength > 0
       |        ))
       |      }
       |
       |      val isLendInitiationServiceCheck = {
       |        allOf(Coll(
       |          blake2b256(lendBox.propositionBytes) == lendBoxHash,
       |          serviceFullCheck,
       |          serviceFeeBox.value >= serviceFee,
       |          serviceFeeBox.propositionBytes == spendingServiceBoxOwnerPubKey.get
       |       ))
       |      }
       |
       |      sigmaProp(isLendInitiationServiceCheck && lendBoxCheck)
       |    } else {
       |      // Lend Success
       |      // Service, LendBox -> Service, RepaymentBox, BorrowerLoanedFunds
       |      if ((OUTPUTS(0).core.SingleLender.Ergs.tokens(1)._2 == SELF.core.SingleLender.Ergs.tokens(1)._2 + 1) &&
       |      OUTPUTS(0).core.SingleLender.Ergs.tokens(2)._2 == SELF.core.SingleLender.Ergs.tokens(2)._2 - 1) {
       |
       |        sigmaProp(allOf(Coll(
       |          blake2b256(OUTPUTS(1).propositionBytes) == repaymentBoxHash,
       |          serviceFullCheck
       |        )))
       |      } else {
       |        // ** Repayment Success **
       |        // Service, Repayment -> Service, ProfitSharing, LenderRepaidFunds
       |        if ((OUTPUTS(0).core.SingleLender.Ergs.tokens(1)._2 == SELF.core.SingleLender.Ergs.tokens(1)._2) &&
       |          OUTPUTS(0).core.SingleLender.Ergs.tokens(2)._2 == SELF.core.SingleLender.Ergs.tokens(2)._2 + 1) {
       |          // Profit Sharing
       |          val profitSharingBox = OUTPUTS(2)
       |          val lendersBox = OUTPUTS(1)
       |
       |          val profitSharingBoxOwnerValidation =
       |            profitSharingBox.propositionBytes == spendingServiceBoxOwnerPubKey.get
       |          val repaymentDetails = INPUTS(1).R8[Coll[Long]]
       |          val repaymentAmount = repaymentDetails.get(1)
       |          val repaymentInterestAmount = repaymentDetails.get(2)
       |
       |          // Interest rate above 1%, ErgoLend takes Profit.
       |          // If Interest rate is at 0%, ErgoLend does Charity too
       |          val fundingInfo = INPUTS(1).R4[Coll[Long]]
       |          val interestRate = fundingInfo.get(2)
       |
       |          val defaulted = repaymentDetails.get(3) < HEIGHT && INPUTS(1).value < repaymentDetails.get(1)
       |
       |          /**
       |            *   Defaulted || Zero Interest Loans
       |            *
       |            *   Defaulted meaning, the repayments are not fully
       |            *   funded, the deadline has passed, and they decide
       |            *   to take their loss.
       |            *
       |            *   For both Defaulted and Zero Interest Loans,
       |            *   ErgoLend will not take a cut.
       |            *
       |            *   Note: We are still taking profit if the loan
       |            *     gets repaid after defaulting.
       |            **/
       |          if (interestRate == 0 || defaulted) {
       |            // There will only be servicebox and lender box (+ miner's fee)
       |            // not putting count of lendcore.boxes for potential credit check
       |            sigmaProp(serviceFullCheck)
       |          } else {
       |            // Take profit
       |            val profitSharingAmount = (repaymentInterestAmount * spendingServiceBoxProfitSharing.get(0)) /1000
       |            val profitSharingAmountValidation = profitSharingBox.value >= profitSharingAmount
       |
       |            // Gives a margin error of minFee
       |            val repaymentLendBoxAmount = repaymentAmount - profitSharingAmount - (2 * minFee)
       |            val repaymentBoxRepaid = lendersBox.value >= repaymentLendBoxAmount
       |
       |            if (profitSharingAmount > minFee) {
       |              sigmaProp(allOf(Coll(
       |                profitSharingAmountValidation,
       |                serviceFullCheck,
       |                profitSharingBoxOwnerValidation,
       |                repaymentBoxRepaid
       |              )))
       |            } else {
       |              sigmaProp(serviceFullCheck && repaymentBoxRepaid)
       |            }
       |          }
       |
       |        } else {
       |          // Refund Lend Box
       |          if ((OUTPUTS(0).core.SingleLender.Ergs.tokens(1)._2 == SELF.core.SingleLender.Ergs.tokens(1)._2 + 1) &&
       |           OUTPUTS(0).core.SingleLender.Ergs.tokens(2)._2 == SELF.core.SingleLender.Ergs.tokens(2)._2) {
       |            sigmaProp(serviceFullCheck)
       |          } else {
       |            sigmaProp(false)
       |          }
       |        }
       |      }
       |    }
       |  }
       |}
       |""".stripMargin
}
