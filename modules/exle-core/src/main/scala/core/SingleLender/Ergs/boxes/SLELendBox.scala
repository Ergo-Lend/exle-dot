package core.SingleLender.Ergs.boxes

import boxes.LendBox
import config.Configs
import contracts.SingleLender.Ergs.SLELendBoxContract
import core.SingleLender.Ergs.boxes.registers.{BorrowerRegister, FundingInfoRegister, LendingProjectDetailsRegister, SingleLenderRegister}
import core.tokens.LendServiceTokens
import org.ergoplatform.appkit._
import special.collection.Coll

/**
 * Depending on what we're doing with it, we may need to add lender tree
 * In a single lender box, there are only 2 states
 * 1. Empty/Not funded - 0 value
 * 2. Funded -  Fully funded value
 *
 * When fully funded, the box will be processed
 *
 * For Single Lender Lending Box,
 * if the funds are
 * - Insufficient -> Proxy contract refunds currency
 * - Perfectly Funded -> Cool
 * - Over Funded -> change is returned to lender
 *
 * @param value
 * @param boxTokenId
 * @param fundingInfoRegister
 * @param lendingProjectDetailsRegister
 * @param singleLenderRegister
 */
case class SLELendBox(value: Long,
                                  fundingInfoRegister: FundingInfoRegister,
                                  lendingProjectDetailsRegister: LendingProjectDetailsRegister,
                                  borrowerRegister: BorrowerRegister,
                                  singleLenderRegister: SingleLenderRegister,
                                  val lendToken: ErgoToken = new ErgoToken(LendServiceTokens.lendToken, 1),
                                  id: ErgoId = ErgoId.create("")) extends LendBox {

  def apply(inputBox: InputBox): SLELendBox = {
    val r4 = inputBox.getRegisters.get(0).getValue.asInstanceOf[Coll[Long]].toArray
    val r5 = inputBox.getRegisters.get(1).getValue.asInstanceOf[Coll[Coll[Byte]]].toArray
    val r6 = inputBox.getRegisters.get(2).getValue.asInstanceOf[Coll[Byte]].toArray
    val r7 = inputBox.getRegisters.get(3).getValue.asInstanceOf[Coll[Byte]].toArray
    val boxToken = inputBox.getTokens.get(0)
    val fundingInfoRegister = new FundingInfoRegister(r4)
    val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(r5)
    val borrowerRegister = new BorrowerRegister(r6)
    val lenderRegister = new SingleLenderRegister(r7)
    new SLELendBox(
      inputBox.getValue,
      fundingInfoRegister,
      lendingProjectDetailsRegister,
      borrowerRegister,
      lenderRegister,
      lendToken = boxToken,
      id = inputBox.getId)
  }

  def this(inputBox: InputBox) = this(
    value = inputBox.getValue,
    fundingInfoRegister = new FundingInfoRegister(inputBox.getRegisters.get(0).getValue.asInstanceOf[Coll[Long]].toArray),
    lendingProjectDetailsRegister = new LendingProjectDetailsRegister(inputBox.getRegisters.get(1).getValue.asInstanceOf[Coll[Coll[Byte]]].toArray),
    borrowerRegister = new BorrowerRegister(inputBox.getRegisters.get(2).getValue.asInstanceOf[Coll[Byte]].toArray),
    singleLenderRegister = if (inputBox.getRegisters.size() > 3)
      new SingleLenderRegister(inputBox.getRegisters.get(3).getValue.asInstanceOf[Coll[Byte]].toArray) else
      SingleLenderRegister.emptyRegister,
    lendToken = inputBox.getTokens.get(0),
    id = inputBox.getId
  )

  def fundBox(lenderAddress: String): SLELendBox = {
    val lenderRegister = new SingleLenderRegister(lenderAddress)
    val fundedValue = getFundingTotalErgs
    new SLELendBox(
      value = fundedValue,
      fundingInfoRegister,
      lendingProjectDetailsRegister,
      borrowerRegister,
      lenderRegister)
  }

  /**
   * Empty/Not Funded State
   * Does not have lendingRegister
   * @param ctx
   * @param txB
   * @return
   */
  def getInitiationOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    val lendBoxContract = SLELendBoxContract.getContract(ctx)
    val lendBox = txB.outBoxBuilder()
      .value(value)
      .contract(lendBoxContract)
      .tokens(lendToken)
      .registers(
        fundingInfoRegister.toRegister,
        lendingProjectDetailsRegister.toRegister,
        borrowerRegister.toRegister).build()

    lendBox
  }

  /**
   * Funded state
   * @param ctx
   * @param txB
   * @return
   */
  def getFundedOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    // If lender is null, then it can't possibly be funded.
    if (singleLenderRegister.lendersAddress.getBytes().deep == Array.emptyByteArray.deep) {
      //@todo create better exception
      throw new Exception("Lender address is empty, but single lender box is funded.")
    }

    val lendBoxContract = SLELendBoxContract.getContract(ctx)
    val lendBox = txB.outBoxBuilder()
      .value(value)
      .contract(lendBoxContract)
      .tokens(lendToken)
      .registers(
        fundingInfoRegister.toRegister,
        lendingProjectDetailsRegister.toRegister,
        borrowerRegister.toRegister,
        singleLenderRegister.toRegister).build()

    lendBox
  }

  override def getOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    if (value >= fundingInfoRegister.fundingGoal) {
      getFundedOutputBox(ctx, txB)
    } else {
      getInitiationOutputBox(ctx, txB)
    }
  }

  /**
   * Total funding value =
   * FundingGoal + RepaymentBoxCreationValue + ProxyContractTxFee + LendBoxFundedTxFee - LendBoxValue
   * @return
   */
  def getFundingTotalErgs: Long = {
    val repaymentBoxCreation = Parameters.MinFee
    val proxyContractTxFee = Parameters.MinFee
    val lendBoxFundedTxFee = Parameters.MinFee
    val totalFundValue = fundingInfoRegister.fundingGoal +
      repaymentBoxCreation +
      proxyContractTxFee +
      lendBoxFundedTxFee - value

    totalFundValue
  }

  def getBorrowersAddress: Address = {
    Address.create(borrowerRegister.borrowersAddress)
  }

  //  def fundedOutBox(ctx: BlockchainContext): OutBox
  //  def fundLendBox(pubkey: String, fundedAmount: Long): OutBox
  //  def addLender(pubkey: String)

  override def getLendersAddress: Address = {
    Address.create(singleLenderRegister.lendersAddress)
  }
}

object SLELendBox {
  def createViaPaymentBox(paymentBox: SingleLenderInitiationPaymentBox): SLELendBox = {
    val lendBoxInitialValue = paymentBox.value - Parameters.MinFee - Configs.serviceFee
    return new SLELendBox(
      lendBoxInitialValue,
      paymentBox.fundingInfoRegister,
      paymentBox.lendingProjectDetailsRegister,
      paymentBox.borrowerRegister,
      SingleLenderRegister.emptyRegister)
  }

  def getLendBoxInitiationPayment: Long = {
    val lendBoxCreation = Parameters.MinFee
    val lendInitiationTxFee = Parameters.MinFee

    val totalPayment = lendBoxCreation + lendInitiationTxFee + Configs.serviceFee

    return totalPayment
  }
}
