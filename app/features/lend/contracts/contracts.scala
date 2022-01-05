package features.lend

package object contracts {
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
   * When a lending box is funded, it doesnt matter if it passed the deadline,
   * it should move to the next phase.
   *
   * If its not funded passed deadline, no funds can be added. Only refunded to lenders
   *
   * If its still funding, it can accept more funding and have an increase in value.
   *
   *
   * Scenario 1: Lend Initiated
   *
   * Funds from proxy contracts are collected and lendbox is initiated
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
   * lendbox. In a singleLender lendBox, the funds have to be equal to the funding goal
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
   * Variables:
   * - fundedHeight
   * - repaymentFundingValue
   * - minFee
   * - repaymentBoxMinAmount
   * - serviceLendToken
   */
  lazy val singleLenderLendingBoxScript: String =
    s"""{
       |  // first output is the funding box always
       |  val lendingBoxFundAccounting = SELF.R4[Coll[Long]]
       |  val lendingBoxFundDetails = SELF.R5[Coll[Coll[Byte]]]
       |  val lendingBoxLenderDetails = SELF.R6[Coll[Byte]]
       |
       |  val fundingGoal = lendingBoxFundAccounting.get(0)
       |  val deadlineHeight = lendingBoxFundAccounting.get(1)
       |  val interestRate = lendingBoxFundAccounting.get(2)
       |  val repaymentHeightLength = lendingBoxFundAccounting.get(3)
       |
       |  val selfValue = SELF.value
       |  val borrowerPk = SELF.R5[Coll[Coll[Byte]]].get(2)
       |
       |  val outputSize = OUTPUTS.size
       |  val inputSize = INPUTS.size
       |
       |  val lendBoxVerification = SELF.tokens(0)._1 == serviceLendToken
       |
       |  // if the lending box is FUNDED.
       |  // Then it should be able to be spent by the borrower and the borrower only. However,
       |  // it needs to be able to output funds to borrower, service fee to lend-team
       |  // and most importantly a repayment box with the right data.
       |  // NOTE: if the borrower does not spend it after a certain amount of time,
       |  //        it goes back to the lenders
       |
       |  if (selfValue >= fundingGoal) {
       |
       |    // Scenario 3: ** FUNDED **
       |    //
       |    // if it is funded, the borrower can spend it
       |    // The repayment box have to have the same details as lendBox
       |    //
       |    // Checks:
       |    // - RepaymentBox having the same details as LendBox
       |    // - RepaymentBox has new detail about repayment info.
       |    // - BorrowerFund: Gets full value from funds
       |
       |    val repaymentBox = OUTPUTS(1)
       |    val repaymentBoxFundAccounting = repaymentBox.R4[Coll[Long]]
       |    val repaymentBoxFundDetails = repaymentBox.R5[Coll[Coll[Byte]]]
       |    val repaymentBoxLenderDetails = repaymentBox.R6[Coll[Byte]]
       |
       |    // 1. funded height, repaymentFundedValue
       |    val repaymentDetails = repaymentBox.R7[Coll[Long]]
       |    val repaymentBoxFundedHeight = repaymentDetails.get(0)
       |    val repaymentBoxFundingValue = repaymentDetails.get(1)
       |    val repaymentBoxFunded
       |
       |
       |    // compare fund details
       |    val fundDetailReplication = allOf(Coll(
       |      repaymentBoxFundDetails.get == lendingBoxFundDetails.get,
       |      repaymentBoxFundAccounting.get == lendingBoxFundAccounting.get,
       |      repaymentBoxLenderDetails.get == lendingBoxLenderDetails.get
       |    ))
       |
       |    val repaymentBoxDetailsInstantiated = {
       |      allOf(Coll(
       |        // funded Height is a variable
       |        repaymentBoxFundedHeight == fundedHeight,
       |        repaymentBoxFundingValue == repaymentFundingValue,
       |      ))
       |    }
       |
       |    // the last box should be the borrowers address. That's where the fund will go
       |    val borrowersFunded = OUTPUTS(outputSize - 1)
       |    val borrowerFundedAmount = selfValue - minFee - repaymentBoxMinAmount
       |
       |    val borrowerBoxFunded = {
       |      allOf(Coll(
       |        // ensure that this is the borrowers wallet
       |        borrowersFunded.propositionBytes == borrowerPk,
       |        borrowersFunded.value == borrowerFundedAmount,
       |      ))
       |    }
       |
       |    sigmaProp(
       |      lendBoxVerification &&
       |      fundDetailReplication &&
       |      borrowerBoxFunded &&
       |      repaymentBoxDetailsInstantiated)
       |
       |    // ** Funding done **
       |
       |  } else {
       |
       |    // ** NOT FUNDED **
       |    // we first check if it passed the deadline
       |    if (HEIGHT >= deadlineHeight) {
       |
       |      // Scenario 4: ** DEADLINE PASSED: REFUND **
       |      // No lending is accepted after this
       |      // we refund the funds to the lenders
       |
       |      // A singleLenderLendingBox only allows one lender
       |      // Therefore it's either funded, or not. There's
       |      // no in between. Therefore, refunds should return
       |      // tokens to serviceBox, and extra ergs back to
       |      // borrower
       |      //
       |      // Inputs: ServiceBox, LendingBox
       |      // Outputs: ServiceBox, Boxes[Borrowers address]
       |      //
       |      // Checks:
       |      // - Refunded box to borrower
       |      // - Refunded box value == LendingBox value - MinFee
       |      // - ServiceBox has its own checks. Therefore not needed here
       |
       |      val inputLendingBox = INPUTS(1)
       |      val refundToBorrowerBox = OUTPUTS(1)
       |
       |      val boxValueToLender = allOf(Coll(
       |        refundToBorrowerBox.propositionBytes == borrowerPk,
       |        refundToBorrowerBox.value == inputLendingBox.value - minFee
       |      ))
       |
       |      sigmaProp(allOf(Coll(boxValueToLender, lendBoxVerification)))
       |
       |    } else {
       |
       |      // Scenario 2: ** LENDING ACTIVE: LEND **
       |      // the fund is still active, and lenders can fund it
       |      // In this part, there are 2 inputs. Lending Box, and Lend Proxy Contract
       |
       |      val inputLendingBox = INPUTS(0)
       |      val outputLendingBox = OUTPUTS(0)
       |      val lendProxyContract = INPUTS(1)
       |
       |      // Ensure details are replicated to the new funding box
       |      val inputLendingBoxAccounting = inputLendingBox.R4[Coll[Long]]
       |      val inputLendingBoxDetails = inputLendingBox.R5[Coll[Coll[Byte]]]
       |      val lenderPk = lendProxyContract.R4[Coll[Byte]]
       |
       |      val outputLendingBoxAccounting = outputLendingBox.R4[Coll[Long]]
       |      val outputLendingBoxDetails = outputLendingBox.R5[Coll[Coll[Byte]]]
       |      val outputLendingBoxLenderPk = outputLendingBox.R6[Coll[Byte]]
       |
       |      val lendingBoxDetailReplicationAndInstantiation = allOf(Coll(
       |        inputLendingBoxAccount == outputLendingBoxAccounting,
       |        inputLendingBoxDetails == outputLendingBoxDetails,
       |        outputLendingBoxLenderPk == lenderPk
       |      ))
       |
       |      // Note: In a single lender lending box, only one lender can lend
       |      //      therefore if the lender funds the box, they have to fund
       |      //      the full amount
       |      val newFundedValue = fundingGoal + minFee + repaymentBoxMinAmount
       |
       |      val valueTransferred = allOf(Coll(
       |        newFundedValue == outputLendingBox.value
       |      ))
       |
       |      // Boxes inputs and outputs
       |      val inputOutputBoxCount = allOf(Coll(
       |        INPUTS.size == 2,
       |        OUTPUTS.size == 1
       |      ))
       |
       |      sigmaProp(
       |        allOf(Coll(
       |          lendBoxVerification,
       |          lendingBoxDetailReplicationAndInstantiation,
       |          valueTransferred,
       |          inputOutputBoxCount,
       |        ))
       |      )
       |
       |      // ** LENDED **
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
   * - serviceRepaymentToken
   * - minFee
   * - fundedHeight
   * - repaymentAmount
   */
  lazy val singleLenderRepaymentBoxScript: String =
    s"""{
       |  // @function
       |  // the function of the repayment box should solely
       |  // be to ensure payment gets accumulated, and that it
       |  // is returned to lender
       |
       |  // if output size == 1, it means its adding funds
       |  val repaymentBoxVerification = SELF.tokens(0)._1 == serviceRepaymentToken
       |
       |  if (OUTPUTS.size == 1) {
       |    // *** ADDING FUNDS ***
       |    // @variables minFee
       |
       |    val repaymentBoxInput = INPUTS(0)
       |    val repaymentBoxOutput = OUTPUTS(0)
       |    val proxyBox = INPUTS(1)
       |
       |    val transferredValue = repaymentBoxInput.value + proxyBox.value - minFee
       |
       |    val isValueTransferred = repaymentBoxOutput.value == transferredValue
       |
       |    val inputRepaymentBoxAccounting = repaymentBoxInput.R4[Coll[Long]]
       |    val inputRepaymentBoxDetails = repaymentBoxInput.R5[Coll[Coll[Byte]]]
       |    val inputRepaymentBoxLender = repaymentBoxInput.R6[Coll[Byte]]
       |    val inputRepaymentBoxRepaymentDetails = repaymentBoxInput.R7[Coll[Long]]
       |
       |    val outputRepaymentBoxAccounting = repaymentBoxOutput.R4[Coll[Long]]
       |    val outputRepaymentBoxDetails = repaymentBoxOutput.R5[Coll[Coll[Byte]]]
       |    val outputRepaymentBoxLender = repaymentBoxOutput.R6[Coll[Byte]]
       |    val outputRepaymentBoxRepaymentDetails = repaymentBoxOutput.R7[Coll[Long]]
       |
       |    val repaymentBoxDetailCheck = {
       |      allOf(Coll(
       |        repaymentBoxVerification,
       |        outputRepaymentBoxAccounting == inputRepaymentBoxAccounting,
       |        outputRepaymentBoxDetails == inputRepaymentBoxDetails,
       |        outputRepaymentBoxLender == inputRepaymentBoxLender,
       |        outputRepaymentBoxRepaymentDetails == inputRepaymentBoxRepaymentDetails
       |      ))
       |    }
       |
       |    val repaymentNotFullyFunded = repaymentBoxInput.value >= SELF.R7[Coll[Long]].get(1)
       |
       |    val repaymentFundingFulfilled = {
       |      allOf(Coll(
       |        isValueTransferred,
       |        repaymentBoxDetailCheck,
       |        repaymentNotFullyFunded
       |      ))
       |    }
       |
       |    sigmaProp(repaymentFundingFulfilled)
       |
       |  } else {
       |
       |    // Scenario 3: Fully funded
       |    // *** REPAYMENT FULFILLED ***
       |    // takes in a service box and repayment box
       |    // outputs 2 boxes, 1 to lender, 1 service box
       |    //
       |    // The conditions that must be true in here is
       |    // service box has the same token + 1
       |    // lenderbox has value, and is the same pk as lender stored
       |    // All the lenderBox care about is whether it refunded the lender.
       |    // let the service box handle itself.
       |
       |    val selfRepaymentBox = SELF
       |    val selfFundingDetails = SELF.R4[Coll[Long]]
       |    val selfLendDetails = SELF.R5[Coll[Coll[Byte]]]
       |    val selfLenderPk = SELF.R6[Coll[Byte]]
       |    val selfRepaymentDetails = SELF.R7[Coll[Long]]
       |
       |    val fundingSuccessful = selfRepaymentBox.value >= selfRepaymentDetails.get(1)
       |    if (fundingSuccessful) {
       |
       |      val inputRepaymentBox = INPUTS(1)
       |      val lenderRepaidBox = OUTPUTS(OUTPUTS.size - 1)
       |
       |      // compare lender's box and value
       |      val inputRepaymentAcc = inputRepaymentBox.R4[Coll[Long]]
       |      val fundingGoal = inputRepaymentAcc.get(0)
       |      val repaymentDetails = inputRepaymentBox.R7[Coll[Long]]
       |      val repaymentGoal = repaymentDetails.get(1)
       |      val lenderPk = inputRepaymentBox.R6[Coll[Byte]]
       |
       |      val lendBoxFunded = {
       |        allOf(Coll(
       |          // ensure that this is the borrowers wallet
       |          outputLenderBox.propositionBytes == lenderPk,
       |          outputLenderBox.value >= repaymentGoal,
       |        ))
       |      }
       |
       |      sigmaProps(lendBoxFunded && repaymentBoxVerification)
       |
       |    } else {
       |
       |      // Scenario 1: Creation
       |      //
       |      // Checks:
       |      // - Tokens
       |      // - LendboxDetails Replicated
       |
       |      val lendBox = INPUTS(1)
       |      val lendBoxFundingDetails = lendBox.R4[Coll[Long]]
       |      val lendBoxLendDetails = lendBox.R5[Coll[Coll[Byte]]]
       |      val lendBoxLenderPk = lendBox.R6[Coll[Byte]]
       |
       |      val lendBoxDetailsReplicatedCheck = allOf(Coll(
       |        repaymentBoxVerification,
       |        lendBoxFundingDetails == selfFundingDetails,
       |        lendBoxLendDetails == selfLendDetails,
       |        lendBoxLenderPk == selfLenderPk
       |      ))
       |
       |      val repaymentDetailsInfoCheck = allOf(Coll(
       |        selfRepaymentDetails.get(0) == fundedHeight,
       |        selfRepaymentDetails.get(1) == repaymentAmount
       |      ))
       |
       |      sigmaProps(allOf(Coll(
       |        repaymentBoxVerification,
       |        lendBoxDetailsReplicatedCheck,
       |        repaymentDetailsInfoCheck
       |      )))
       |    }
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
   *
   * Scenario 3 -> Lend Funded
   * Service box switches LendToken to RepaymentToken
   * Box to Lender
   *
   * Scenario 4 -> Repayment Consumption
   * Service Box receives token from Repayment Box
   * Service Box uses PubKey and ProfitSharingPercentage to create
   * ProfitSharingBox to ErgoLend
   *
   * Scenario 5 -> Refunds
   * Refunds only happen when lendbox is not successful
   * In the refunds, the lendToken is return to the serviceBox
   *
   * Input variables:
   * - serviceNFT
   * - serviceLendToken
   * - serviceRepaymentToken
   */
  lazy val singleLenderLendServiceBoxScript: String =
    s"""{
       |
       |  val spendingServiceBox = SELF
       |  val outputServiceBox = OUTPUTS(0)
       |
       |  val spendingServiceBoxOwnerPubKey = spendingServiceBox.R4[Coll[Byte]].get
       |  val spendingServiceBoxProfitSharingPercentage = spendingServiceBox.R5[Long].get
       |  val spendingServiceBoxNFT = spendingServiceBox.tokens(0)._1
       |  val spendingServiceBoxLendToken = spendingServiceBox.tokens(1)._1
       |  val spendingServiceBoxLendTokenCount = spendingServiceBox.tokens(1)._2
       |  val spendingServiceBoxRepaymentToken = spendingServiceBox.tokens(2)._1
       |  val spendingServiceBoxRepaymentTokenCount = spendingServiceBox.tokens(2)._2
       |  val spendingServiceBoxValue = spendingServiceBox.value
       |
       |
       |  val outputServiceBoxOwnerPubKey = outputServiceBox.R4[Coll[Byte]].get
       |  val outputServiceBoxProfitSharingPercentage = outputServiceBox.R5[Long].get
       |  val outputServiceBoxNFT = outputServiceBox.tokens(0)._1
       |  val outputServiceBoxLendToken = outputServiceBox.tokens(1)._1
       |  val outputServiceBoxLendTokenCount = outputServiceBox.tokens(1)._2
       |  val outputServiceBoxRepaymentToken = outputServiceBox.tokens(2)._1
       |  val outputServiceBoxRepaymentTokenCount = outputServiceBox.tokens(2)._2
       |  val outputServiceBoxValue = outputServiceBox.value
       |
       |
       |  // Scenario 1: Mutation
       |  //
       |  // Owner can mutate the box, but the amount of boxToken
       |  // have to always be the same.
       |
       |  val isMutable = spendingServiceBoxLendTokenCount == outputServiceBoxLendTokenCount
       |  if (isMutable) {
       |    ownerPK
       |  } else {
       |
       |      // Service Checks
       |      //
       |      // ServiceBox: (not altered)
       |      // - OwnerPubkey: not altered
       |      // - ProfitSharingPercentage: not altered
       |      // - propositionBytes: not altered
       |      // - lendToken id: correct and not altered
       |      // - nft id: correct and not altered
       |      // - repaymentToken id: correct and not altered
       |      // - value: not altered
       |
       |      val serviceCheck = {
       |        allOf(Coll(
       |          spendingServiceBoxOwnerPubKey == outputServiceBoxOwnerPubKey,
       |          spendingServiceBoxProfitSharingPercentage == outputServiceBoxProfitSharingPercentage,
       |          spendingServiceBox.propositionBytes == outputServiceBox.propositionBytes,
       |          spendingServiceBoxNFT == serviceNFT,
       |          spendingServiceBoxLendToken == serviceLendToken,
       |          spendingServiceBoxRepaymentToken == serviceRepaymentToken,
       |          spendingServiceBoxNFT == outputServiceBoxNFT,
       |          spendingServiceBoxLendToken == outputServiceBoxLendToken,
       |          spendingServiceBoxRepaymentToken == outputServiceBoxRepaymentToken,
       |          spendingServiceBoxValue == outputServiceBoxValue
       |        ))
       |      }
       |
       |    // Scenario 2: Lend Initiation
       |    //
       |    // spendingBox have one extra token as compared to outputBox
       |    // Repayment token is the same
       |    //
       |    // Checks:
       |    // - Service Check
       |    // - Lend token == 1 in lend box
       |    // - Service Box lend token == -1 of total
       |
       |    val isLendInitiation =
       |      (spendingServiceBoxLendTokenCount == outputServiceBoxLendTokenCount - 1) &&
       |      (spendingServiceBoxRepaymentTokenCount == outputServiceBoxRepaymentTokenCount)
       |
       |    if (isLendInitiation) {
       |      val tokenizedBox = OUTPUTS(1)
       |      val tokenizedBoxLendToken = tokenizedBox.tokens(0)._1
       |      val tokenizedBoxLendTokenCount = tokenizedBox.tokens(0)._2
       |
       |      val isLendInitiationServiceCheck = {
       |        allOf(Coll(
       |          serviceCheck,
       |          tokenizedBoxLendToken == spendingServiceBoxLendToken,
       |          tokenizedBoxLendTokenCount == 1
       |       ))
       |      }
       |      sigmaProps(isLendInitiationCheck)
       |    }
       |
       |
       |    // Scenario 3: Lend Funding Success
       |    //
       |    // Lend funding success, replace lend token with repayment token
       |    //
       |    // Checks:
       |    // - Service box: Lend token + 1
       |    // - Service box: Repayment token - 1
       |    // - LendBox: Value >= funding goal
       |    // - LendBox: Lend Token == 1 (was 1 before tx)
       |    // - RepaymentBox: Repayment token == 1
       |    //
       |    //    Borrower Box
       |    // - Value: >= lendingFund
       |    // - PropositionBytes == Borrowers
       |
       |    val isLendFundingSuccess =
       |      (spendingServiceBoxLendTokenCount == outputServiceBoxLendTokenCount + 1) &&
       |      (spendingServiceBoxRepaymentTokenCount == outputServiceBoxRepaymentTokenCount - 1)
       |
       |    if (isLendFundingSuccess) {
       |      val lendingBox = INPUTS(1)
       |      val repaymentBox = OUTPUTS(1)
       |
       |      val tokenizedBoxLendToken = lendingBox.tokens(0)._1
       |      val tokenizedBoxLendTokenCount = lendingBox.tokens(0)._2
       |      val lendingValue = lendingBox.R4[Coll[Long]].get(0)
       |      val lendingBoxValue = lendingBox.value
       |
       |      val tokenizedBoxRepaymentToken = repaymentBox.tokens(0)._1
       |      val tokenizedBoxRepaymentTokenCount = repaymentBox.tokens(0)._2
       |
       |      val isLendFundingServiceCheck = {
       |        allOf(Coll(
       |          serviceCheck,
       |          tokenizedBoxLendToken == spendingServiceBoxLendToken,
       |          tokenizedBoxLendTokenCount == 1,
       |          tokenizedBoxRepaymentToken = spendingServiceBoxRepaymentToken,
       |          tokenizedBoxRepaymentTokenCount = 1,
       |          lendingBoxValue >= lendingValue
       |        ))
       |      }
       |
       |      // Check send to
       |      // borrower
       |      //
       |      // Validate that the funds box goes to
       |      // borrower
       |
       |      val borrowerBox = OUTPUTS(2)
       |      val borrowerPubKey = lendingBox.R6[Coll[Byte]].get
       |      val borrowerBoxValue = borrowerBox.value
       |
       |      val borrowerBoxValidation =
       |        allOf(Coll(
       |          borrowerBox.propositionBytes == borrowerPubKey,
       |          borrowerBoxValue >= lendingValue
       |        ))
       |
       |      sigmaProps(allOf(Coll(
       |        isLendFundingServiceCheck,
       |        borrowerBoxValidation
       |      )))
       |    }
       |
       |
       |    // Scenario 4: Repayment Consumption
       |    //
       |    // outputBox will receive one token from lendBox
       |    // generates profitSharingBox to owner
       |    // Checks:
       |    // Total Output Box: 3 (ServiceBox, ProfitSharingBox, Lender's Box)
       |    // Service box: Lend Token == same
       |    // Service box: Repayment Token + 1
       |    // Profit Sharing Box: PropositionBytes == profitSharingBoxOwnerPubKey
       |    // RepaymentBox: Value >= repaymentValue
       |
       |    val isRepaymentConsumption =
       |      (spendingServiceBoxLendTokenCount == outputServiceBoxLendTokenCount) &&
       |      (spendingServiceBoxRepaymentTokenCount == outputServiceBoxRepaymentTokenCount + 1)
       |
       |
       |    // Verify repayment token and repayment box
       |    if (isRepaymentConsumption) {
       |      // @todo verify repayment value
       |      val tokenizedBox = INPUTS(1)
       |      val tokenizedBoxLendToken = tokenizedBox.tokens(0)._1
       |      val tokenizedBoxLendTokenCount = tokenizedBox.tokens(0)._2
       |      val repaymentValue = tokenizedBox.R7[Coll[Long]].get(2)
       |      val repaymentBoxValue = tokenizedBox.value
       |
       |      val isRepaymentConsumptionServiceCheck = {
       |        allOf(Coll(
       |          serviceCheck,
       |          tokenizedBoxRepaymentToken == spendingServiceBoxRepaymentToken,
       |          tokenizedBoxRepaymentTokenCount == 1,
       |          repaymentBoxValue >= repaymentValue
       |        ))
       |      }
       |
       |
       |      // Profit Sharing
       |      //
       |      // Validate that the profit sharing box goes to
       |      // owner
       |
       |      val profitSharingBox = OUTPUTS(1)
       |
       |      val profitSharingBoxOwnerValidation =
       |        profitSharingBox.propositionBytes == spendingServiceBoxOwnerPubKey
       |
       |      sigmaProps(allOf(Coll(
       |        isRepaymentConsumptionServiceCheck,
       |        profitSharingBoxOwnerValidation
       |      )))
       |    }
       |
       |
       |    // Scenario 5: Refunds
       |    //
       |    // if we don't go through any of the above scenario, most
       |    // likely we're going through a refund. Refund only requires
       |    // lendBox
       |    //
       |    // Checks:
       |    // - LendToken return to ServiceBox
       |
       |    val lendingBox = INPUTS(1)
       |    val lendBoxDeadline = lendingBox.R4[Coll[Long]].get(1)
       |
       |    val pastLendDeadline = HEIGHT > lendBoxDeadline
       |    val isLendRefund =
       |      (spendingServiceBoxLendTokenCount == outputServiceBoxLendTokenCount + 1) &&
       |      (spendingServiceBoxRepaymentTokenCount == outputServiceBoxRepaymentTokenCount)
       |
       |    if (pastLendDeadline && isLendRefund) {
       |      val lendingBoxToken = lendingBox.tokens(0)._1
       |
       |      val refundCheck = allOf(Coll(
       |        lendingBoxToken == serviceLendToken,
       |        spendingServiceBoxLendTokenCount == outputServiceBoxLendTokenCount + 1
       |      ))
       |    }
       |
       |    // Scenario: Fail
       |    sigmaProps(false)
       |}
       |""".stripMargin
}
