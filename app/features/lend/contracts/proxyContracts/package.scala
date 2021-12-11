package features.lend.contracts

package object proxyContracts {
  /**
   * Test Refund Proxy Script
   */
  lazy val testProxyScript: String =
    s"""{
       |  val returnFee = {
       |    allOf(Coll(
       |      OUTPUTS(0).value >= (INPUTS(0).value - minFee),
       |      OUTPUTS(0).propositionBytes == fundersPk,
       |      HEIGHT > refundHeightThreshold
       |    ))
       |  }
       |
       |  sigmaProp(returnFee)
       |}
       |""".stripMargin

  /**
   * Lending Box Creation Proxy
   */
  lazy val createLendingBoxProxyScript: String =
    s"""{
       | // the amount of boxes as outputs, else return
       | if (OUTPUTS.size > 1) {
       |  val createLendConditions = {
       |    allOf(Coll(
       |      OUTPUTS(0).tokens(0)._1 == lendServiceNFT,
       |      OUTPUTS(1).tokens(0)._1 == lendServiceToken,
       |      OUTPUTS(1).R4[Coll[Long]].get(0) == goal,
       |      OUTPUTS(1).R4[Coll[Long]].get(1) == deadlineHeight,
       |      OUTPUTS(1).R4[Coll[Long]].get(2) == interestRate,
       |      OUTPUTS(1).R4[Coll[Long]].get(3) == repaymentHeightLength,
       |      OUTPUTS(1).R5[Coll[Coll[Byte]]].get(2) == borrowerPk
       |    ))
       |  }
       |
       |  sigmaProp(createLendConditions)
       | } else {
       |  val returnCreateLendFee = {
       |    allOf(Coll(
       |      OUTPUTS(0).value >= (INPUTS(0).value - minFee),
       |      OUTPUTS(0).propositionBytes == borrowerPk,
       |      HEIGHT > refundHeightThreshold,
       |      // We need to ensure that the service box comes out too
       |      OUTPUTS.size == 2
       |    ))
       |  }
       |
       |  sigmaProp(returnCreateLendFee)
       | }
       |}
       |""".stripMargin

  /**
   * Funding box proxy script
   * When funding, the output is the funding box with increased value.
   */
  lazy val fundLendingBoxProxyScript: String =
    s"""{
       |  // send lending payment to funding box
       |
       |  val outputLendBox = OUTPUTS(0)
       |  val inputLendBox = INPUTS(0)
       |
       |  val deadlineHeight = outputLendBox.R4[Coll[Long]].get(1)
       |  val outputLendBoxId = outputLendBox.R5[Coll[Coll[Byte]]].get(3)
       |  val inputLendBoxId = inputLendBox.R5[Coll[Coll[Byte]]].get(3)
       |
       |  // if its the right box, and deadline has not been reached
       |  // then it can be fundable
       |  if (tokenId == inputLendBoxId && deadlineHeight > HEIGHT) {
       |
       |    val fundingValue = SELF.value
       |    val newFundedValue = inputLendBox.value + fundingValue - minFee
       |
       |    val fundLendBox = {
       |      allOf(Coll(
       |        // we literally just make sure that the number added is the equivalent to
       |        // input funding box + proxy
       |        outputLendBox.value == newFundedValue,
       |        fundId == outputLendBoxId,
       |        // we do the fund box checking through funding box script.
       |      ))
       |    }
       |
       |    sigmaProp(fundLendBox)
       |
       |  } else {
       |    // we return the funds
       |
       |    val returnFunding = {
       |      val total = INPUTS.fold(0L, {(x:Long, b:Box) => x + b.value})
       |      allOf(Coll(
       |        OUTPUTS(0).value >= (total - minFee),
       |        OUTPUTS(0).propositionBytes == lenderAddress,
       |        (total < lendAmountPlusFee || deadlineHeight < HEIGHT),
       |        OUTPUTS.size == 2
       |      ))
       |    }
       |
       |    sigmaProp(returnFunding)
       |  }
       |}
       |""".stripMargin

  /**
   * When repaying the loan
   * Borrower will pay to a proxy script which will merge with Repayment box
   * to output a repayment box.
   *
   * It might be useful for us to compare it to service box too.
   *
   * For a proxy script, we find the minimal it needs for the contract to work. It
   * shouldnt be too complicated.
   *
   * For this one, we only compare it to the data within the repayment box output/input,
   * we just check if its the right box, and also check if the fund is right. Cause all we're
   * doing is transferring the value from this loan to the box.
   *
   * Therefore these are the things to check:
   * 1. Is it the right fund/repayment box (this includes, project name, description
   * 2. Does the fund exceed what they're supposed to pay back. (do we even need this? Maybe someone might wanna pay back more)
   */
  lazy val repayLoanProxyScript: String =
    s"""
       |{
       |  val inputRepaymentBox = INPUTS(0)
       |  val outputRepaymentBox = OUTPUTS(0)
       |  val repaymentBoxFundingDetails = inputRepaymentBox.R4[Coll[Long]]
       |  val repaymentBoxInfo = inputRepaymentBox.R5[Coll[Coll[Byte]]]
       |
       |  val repaymentGoal = repaymentBoxFundingDetails.get(6)
       |
       |  // compare with fundId to know if the box is the right box
       |  val repaymentBoxId = repaymentBoxInfo.get(3)
       |
       |  val amountToRepay = SELF.value
       |
       |  // this has to consider transaction fee
       |  val amountRepaid = inputRepaymentBox.value
       |
       |  val amountRepaidOutput = amountToRepay + amountRepaid
       |
       |  // if it is the right box, and the repayment goal has not been reached (amountRepaid smaller than repaymentGoal)
       |  if (fundId == repaymentBoxId && repaymentGoal > amountRepaid) {
       |
       |    // we check the id and if the value is correct.
       |    val repayment = {
       |      allOf(Coll(
       |        fundId == repaymentBoxId,
       |        outputRepaymentBox.value == amountRepaidOutput - minFee, // The value should be the same Inputs and outputs (only 1 box is output)
       |      ))
       |    }
       |
       |    sigmaProp(repayment)
       |
       |  } else {
       |    // else refund the amount repaid (this refund will not be transacted alongside an input of repayment box)
       |    // it has to be processed purely as a refund. Therefore it will be used as the only input
       |    val returnRepayment = {
       |      val total = INPUTS.fold(0L, {(x:Long, b:Box) => x + b.value})
       |      allOf(Coll(
       |        OUTPUTS(0).value >= (total - minFee),
       |        OUTPUTS(0).propositionBytes == userAddress, // user must receive the transaction back to his account
       |        (repaymentGoal < amountRepaid || fundId != repaymentBoxId),
       |        OUTPUTS.size == 2 // refund box and transaction fee box
       |      ))
       |    }
       |
       |    sigmaProp(returnRepayment)
       |  }
       |}
       |""".stripMargin
}
