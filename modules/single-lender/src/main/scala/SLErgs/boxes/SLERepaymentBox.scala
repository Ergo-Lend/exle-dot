package SLErgs.boxes

import SLErgs.LendServiceTokens
import SLErgs.contracts.SLERepaymentBoxContract
import SLErgs.registers.{
  BorrowerRegister,
  FundingInfoRegister,
  LendingProjectDetailsRegister,
  RepaymentDetailsRegister,
  SingleLenderRegister
}
import commons.boxes.RepaymentBox
import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit._
import special.collection.Coll

/**
  * RepaymentBox: Single Lender
  * In a SLERepaymentBox there are 3 states
  * 1. Empty - 0 value
  * 2. Partially Funded - Value less than completed value
  * 3. Fully Funded
  *
  * This repayment box consists of only a single lender.
  * The repayment box can be funded incrementally till it reaches it's repayment goal.
  *
  * @param fundingInfoRegister Register that stores the funding information
  * @param lendingProjectDetailsRegister Register that stores the details of the project for the loan
  * @param singleLenderRegister Register that stores 1 lender
  * @param repaymentDetailsRegister Register that stores the Repayment Details
  */
case class SLERepaymentBox(
  value: Long = 0,
  fundingInfoRegister: FundingInfoRegister,
  lendingProjectDetailsRegister: LendingProjectDetailsRegister,
  borrowerRegister: BorrowerRegister,
  singleLenderRegister: SingleLenderRegister,
  repaymentDetailsRegister: RepaymentDetailsRegister,
  repaymentToken: ErgoToken =
    new ErgoToken(LendServiceTokens.repaymentToken, 1),
  id: ErgoId = ErgoId.create("")
) extends RepaymentBox {

  val repaymentBoxToken = new ErgoToken(LendServiceTokens.repaymentToken, 1)

  def this(inputBox: InputBox) = this(
    value = inputBox.getValue,
    fundingInfoRegister = new FundingInfoRegister(
      inputBox.getRegisters.get(0).getValue.asInstanceOf[Coll[Long]].toArray
    ),
    lendingProjectDetailsRegister = new LendingProjectDetailsRegister(
      inputBox.getRegisters
        .get(1)
        .getValue
        .asInstanceOf[Coll[Coll[Byte]]]
        .toArray
    ),
    borrowerRegister = new BorrowerRegister(
      inputBox.getRegisters.get(2).getValue.asInstanceOf[Coll[Byte]].toArray
    ),
    singleLenderRegister = new SingleLenderRegister(
      inputBox.getRegisters.get(3).getValue.asInstanceOf[Coll[Byte]].toArray
    ),
    repaymentDetailsRegister = new RepaymentDetailsRegister(
      inputBox.getRegisters.get(4).getValue.asInstanceOf[Coll[Long]].toArray
    ),
    repaymentToken = inputBox.getTokens.get(0),
    id = inputBox.getId
  )

  def this(singleLenderLendBox: SLELendBox, fundedHeight: Long) = this(
    value = Parameters.MinFee,
    fundingInfoRegister = singleLenderLendBox.fundingInfoRegister,
    lendingProjectDetailsRegister =
      singleLenderLendBox.lendingProjectDetailsRegister,
    borrowerRegister = singleLenderLendBox.borrowerRegister,
    singleLenderRegister = singleLenderLendBox.singleLenderRegister,
    repaymentDetailsRegister = RepaymentDetailsRegister.apply(
      fundedHeight,
      singleLenderLendBox.fundingInfoRegister
    )
  )

  /**
    * Returns the output box for repayment
    *
    * if the box value is 0, it means it is instantiated. Therefore we return an empty repayment box
    * if the box value is < repaymentAmount, we return a partially fund box
    * if the box value is >= repaymentAmount, we return the box with the value
    *
    * as long as the repayment is funded, it doesn't matter whether it is over-funded, we still repay the lender
    * whatever amount it has in it.
    *
    * @param ctx
    * @param txB
    * @return
    */
  override def getOutputBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder
  ): OutBox = {
    var boxValue = value
    if (boxValue == 0L) {
      boxValue = Parameters.MinFee
    }

    //@todo add tokens
    val repaymentBoxContract = SLERepaymentBoxContract.getContract(ctx)
    val repaymentBox = txB
      .outBoxBuilder()
      .value(boxValue)
      .contract(repaymentBoxContract)
      .tokens(
        repaymentToken
      )
      .registers(
        fundingInfoRegister.toRegister,
        lendingProjectDetailsRegister.toRegister,
        borrowerRegister.toRegister,
        singleLenderRegister.toRegister,
        repaymentDetailsRegister.toRegister
      )
      .build()

    repaymentBox
  }

  /**
    * When we fund a box, we give them a new SLERepaymentBox
    * @param fundingAmount
    * @return
    */
  override def fundBox(fundingAmount: Long): SLERepaymentBox =
    new SLERepaymentBox(
      value = value + fundingAmount,
      fundingInfoRegister = fundingInfoRegister,
      lendingProjectDetailsRegister = lendingProjectDetailsRegister,
      borrowerRegister = borrowerRegister,
      singleLenderRegister = singleLenderRegister,
      repaymentDetailsRegister = repaymentDetailsRegister,
      repaymentToken = repaymentToken
    )

  override def fundedBox(): SLERepaymentBox =
    new SLERepaymentBox(
      value = repaymentDetailsRegister.repaymentAmount + Parameters.MinFee,
      fundingInfoRegister = fundingInfoRegister,
      lendingProjectDetailsRegister = lendingProjectDetailsRegister,
      borrowerRegister = borrowerRegister,
      singleLenderRegister = singleLenderRegister,
      repaymentDetailsRegister = repaymentDetailsRegister,
      repaymentToken = repaymentToken
    )

  def repaidLendersPaymentBox(ergoLendInterest: Long): FundsToAddressBox =
    new FundsToAddressBox(
      value - ergoLendInterest - Parameters.MinFee,
      singleLenderRegister.lendersAddress
    )

  /**
    * RepaymentBox -> Repayment Value
    * Repayment Value -  ProxyMergeTx - Repayment Box Completion
    * @return
    */
  def getFullFundAmount: Long = {
    val fullFundAmount = repaymentDetailsRegister.repaymentAmount
    val proxyMergeTxAndCompletionAmount = Parameters.MinFee * 2
    val totalAmount = fullFundAmount + proxyMergeTxAndCompletionAmount - value
    return totalAmount
  }

  def getFundAmount(amount: Long = 0): Long =
    if (amount == 0) getFullFundAmount
    else {
      val proxyMergeTxAndCompletionAmount = Parameters.MinFee * 2
      return amount + proxyMergeTxAndCompletionAmount
    }

  override def getLendersAddress: ErgoAddress =
    Address.create(singleLenderRegister.lendersAddress).getErgoAddress

  override def getRepaymentInterest: Long =
    repaymentDetailsRegister.totalInterestAmount
}
