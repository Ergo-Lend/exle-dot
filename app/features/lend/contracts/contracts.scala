package features.lend

package object contracts {
  /**
   * Funding Box
   *
   * For this script, we have to consider a few scenarios.
   * 1. Still Funding
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
   */
  lazy val lendingBoxScript: String =
    s"""{
       |  // first output is the funding box always
       |  val lendingBoxFundAccounting = SELF.R4[Coll[Long]]
       |  val lendingBoxFundDetails = SELF.R5[Coll[Coll[Byte]]]
       |  val fundingGoal = lendingBoxFundAccounting.get(0)
       |  val deadlineHeight = lendingBoxFundAccounting.get(1)
       |  val interestRate = lendingBoxFundAccounting.get(2)
       |  val repaymentHeightLength = lendingBoxFundAccounting.get(3)
       |  val serviceFee = lendingBoxFundAccounting.get(4)
       |
       |  val selfValue = SELF.value
       |  val borrowerPk = SELF.R5[Coll[Coll[Byte]]].get(2)
       |
       |  val outputSize = OUTPUTS.size
       |  val inputSize = INPUTS.size
       |
       |  // if the lending box is FUNDED.
       |  // Then it should be able to be spent by the borrower and the borrower only. However,
       |  // it needs to be able to output funds to borrower, service fee to lend-team
       |  // and most importantly a repayment box with the right data.
       |  // NOTE: if the borrower does not spend it after a certain amount of time,
       |  //        it goes back to the lenders
       |
       |  if (selfValue >= fundingGoal) {
       |    // ** FUNDED **
       |
       |    // if it is funded, the borrower can spend it
       |    // There's 3 box to consider, Repayment box, service fee box, service
       |
       |    // in the case where lendbox is funded, repayment box will be first output
       |    val repaymentBox = OUTPUTS(0)
       |    val repaymentBoxFundAccounting = repaymentBox.R4[Coll[Long]]
       |    val repaymentBoxFundDetails = repaymentBox.R5[Coll[Coll[Byte]]]
       |
       |    // 1. funded height, repaymentFundedValue
       |    val repaymentDetails = repaymentBox.R7[Coll[Long]]
       |    val repaymentBoxFundedHeight = repaymentDetails.get(0)
       |    val repaymentBoxFundedValue = repaymentDetails.get(1)
       |
       |    // @todo add lenders
       |
       |    // compare fund details
       |    val fundDetailReplication = allOf(Coll(
       |      repaymentBoxFundDetails.get == lendingBoxFundDetails.get,
       |      repaymentBoxFundAccounting.get == lendingBoxFundAccounting.get,
       |    ))
       |
       |    val repaymentBoxCreated = {
       |      allOf(Coll(
       |        // funded Height is a variable
       |        repaymentBoxFundedHeight == fundedHeight,
       |        repaymentBoxFundedValue == fundedValue,
       |      ))
       |    }
       |
       |    // @TODO ergo-lend service gets paid
       |
       |    // the last box should be the borrowers address. That's where the fund will go
       |    val borrowerBoxFunded = {
       |      val borrowersFunded = OUTPUTS(outputSize - 1)
       |      val borrowerFundedAmount = selfValue - serviceFee - minFee
       |      allOf(Coll(
       |        // ensure that this is the borrowers wallet
       |        borrowersFunded.propositionBytes == borrowerPk,
       |        borrowersFunded.value == borrowerFundedAmount,
       |      ))
       |    }
       |
       |    // @todo Ensure that only borrower can spend it
       |
       |    sigmaProp(
       |      fundDetailReplication &&
       |      borrowerBoxFunded &&
       |      repaymentBoxCreated)
       |
       |    // ** Funding done **
       |
       |  } else {
       |
       |    // ** NOT FUNDED **
       |    // we first check if it passed the deadline
       |    if (HEIGHT >= deadlineHeight) {
       |
       |      // ** DEADLINE PASSED: REFUND **
       |      // No lending is accepted after this
       |      // we refund the funds to the lenders
       |
       |      // In this stage, Lending Box returns the
       |      // funds to lenders, and the box is destroyed.
       |      // Inputs: LendingBox, ServiceBox
       |      // Outputs: ServiceBox, Boxes[lenders addresses]
       |
       |      // For this version, we will just refund refund it back to user
       |      val inputLendingBox = INPUTS(0)
       |      val inputServiceBox = INPUTS(1)
       |      val outputLenderBox = OUTPUTS(0)
       |      val outputServiceBox = OUTPUTS(1)
       |
       |      val boxValueToLender = allOf(Coll(
       |        outputLenderBox.propositionBytes == lenderPk,
       |        outputLenderBox.value == inputLendingBox.value
       |      ))
       |
       |      sigmaProp(boxValueToLender)
       |
       |    } else {
       |
       |      // ** LENDING ACTIVE: LEND **
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
       |      val outputLendingBoxAccounting = outputLendingBox.R4[Coll[Long]]
       |      val outputLendingBoxDetails = outputLendingBox.R5[Coll[Coll[Byte]]]
       |
       |      val lendingBoxDetailReplication = allOf(Coll(
       |        inputLendingBoxAccount == outputLendingBoxAccounting,
       |        inputLendingBoxDetails == outputLendingBoxDetails
       |      ))
       |
       |      // Value comparison
       |      val newFundedValue = inputLendingBox.value + lendProxyContract.value - minFee
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
       |        lendingBoxDetailReplication,
       |        valueTransferred,
       |        inputOutputBoxCount,
       |      )
       |
       |      // ** LENDED **
       |    }
       |  }
       |}
       |""".stripMargin

  lazy val repaymentBoxScript: String =
    s"""{
       |  // there are 2 scenarios
       |  // fund or complete
       |
       |  // we can identify this by the hash of the proxyscript
       |  // output will be 1 only when repaying
       |
       |  // @function
       |  // the function of the repayment box should solely
       |  // be to ensure payment gets accumulated, and that it
       |  // is returned to lender
       |
       |  // if size == 1, it means its adding funds
       |
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
       |    val inputRepaymentBoxFundedHeight = repaymentBoxInput.R7[Long]
       |    val outputRepaymentBoxAccounting = repaymentBoxOutput.R4[Coll[Long]]
       |    val outputRepaymentBoxDetails = repaymentBoxOutput.R5[Coll[Coll[Byte]]]
       |    val outputRepaymentBoxLender = repaymentBoxOutput.R6[Coll[Byte]]
       |    val outputRepaymentBoxFundedHeight = repaymentBoxOutput.R7[Long]
       |
       |    val lendingBoxDetailReplication = {
       |      allOf(Coll(
       |        outputRepaymentBoxAccounting == inputRepaymentBoxAccounting,
       |        outputRepaymentBoxDetails == inputRepaymentBoxDetails,
       |        outputRepaymentBoxLender == inputRepaymentBoxLender,
       |      ))
       |    }
       |
       |    if (INPUTS(0).propositionBytes != SELF.propositionBytes) {
       |      // *** CREATION ***
       |      // if the box does not have the same propositionBytes, it means
       |      // that we're at the creation stage
       |      // in this part, we need to make sure the lending box details are copied over,
       |      // in addition with funded height
       |      // since both has same registers up till 6, we can just use the one at the top
       |
       |      val fundedHeight = outputRepaymentBoxFundedHeight == HEIGHT
       |
       |      sigmaProp(lendingBoxDetailReplication && fundedHeight)
       |    }
       |
       |    val repaymentBoxDetailReplication = inputRepaymentBoxFundedHeight == outputRepaymentBoxFundedHeight
       |
       |    val repaymentFulfilled = {
       |      allOf(Coll(
       |        isValueTransferred,
       |        lenderBoxDetailReplication,
       |        repaymentBoxDetailReplication
       |      ))
       |    }
       |
       |    sigmaProp(repaymentFulfilled)
       |  } else {
       |
       |    // *** REPAYMENT FULFILLED ***
       |    // takes in a service box and repayment box
       |    // outputs 2 boxes, 1 to lender, 1 service box
       |    //
       |    // The conditions that must be true in here is
       |    // service box has the same token + 1
       |    // lenderbox has value, and is the same pk as lender stored
       |    // All the lenderBox care about is whether it refunded the lender
       |    // let the service box handle itself.
       |
       |
       |    val inputRepaymentBox = INPUTS(0)
       |    val outputLenderBox = OUTPUTS(0)
       |
       |    // compare lender's box and value
       |    val inputRepaymentAcc = inputRepaymentBox.R4[Coll[Long]]
       |    val fundingGoal = inputRepaymentAcc.get(0)
       |    val fundedHeight = inputRepaymentBox.R7[Long].get
       |    val interestRate = inputRepaymentAcc.get(2)
       |    val repaymentTimeLength = inputRepaymentAcc.get(3)
       |    val interest = fundingGoal * (interestRate / 100)
       |
       |    val fundingRequired = fundingGoal + interest + interestOverTime
       |
       |    val lendBoxFunded = {
       |      allOf(Coll(
       |        // ensure that this is the borrowers wallet
       |        outputLenderBox.propositionBytes == lenderPk,
       |        outputLenderBox.value >= fundingRequired,
       |      ))
       |    }
       |
       |    sigmaProp(
       |      lendBoxFunded &&
       |      OUTPUTS.size == 2)
       |  }
       |}
       |""".stripMargin

  lazy val lendServiceBoxScript: String =
    s"""{
       |  val serviceBox = OUTPUTS(0)
       |
       |  // If tokens is equal
       |  if (serviceBox.tokens(1)._2 == SELF.tokens(1)._2) {
       |    ownerPK
       |  } else {
       |
       |
       |    if (serviceBox.tokens(1)._2 == SELF.tokens(1)._2 + 1L) {
       |      val serviceCheck = {
       |        allOf(Coll(
       |        ))
       |      }
       |      sigmaProps(serviceCheck)
       |    } else {
       |    }
       |  }
       |}
       |""".stripMargin
}
