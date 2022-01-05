package features.lend.boxes

import config.Configs
import ergotools.LendServiceTokens
import features.lend.boxes.registers.{FundingInfoRegister, LendingProjectDetailsRegister, SingleLenderRegister}
import features.lend.contracts.singleLenderLendingBoxScript
import org.ergoplatform.ErgoAddress
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
case class SingleLenderLendingBox(value: Long,
                                  fundingInfoRegister: FundingInfoRegister,
                                  lendingProjectDetailsRegister: LendingProjectDetailsRegister,
                                  singleLenderRegister: SingleLenderRegister,
                                  val lendToken: ErgoToken = new ErgoToken(LendServiceTokens.lendToken, 1),
                                  id: ErgoId = ErgoId.create("")) extends LendingBox {

  def apply(inputBox: InputBox): SingleLenderLendingBox = {
    val r4 = inputBox.getRegisters.get(0).getValue.asInstanceOf[Array[Long]]
    val r5 = inputBox.getRegisters.get(1).getValue.asInstanceOf[Array[Coll[Byte]]]
    val r6 = inputBox.getRegisters.get(2).getValue.asInstanceOf[Array[Byte]]
    val boxToken = inputBox.getTokens.get(0)
    val fundingInfoRegister = new FundingInfoRegister(r4)
    val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(r5)
    val lenderRegister = new SingleLenderRegister(r6)
    new SingleLenderLendingBox(
      inputBox.getValue,
      fundingInfoRegister,
      lendingProjectDetailsRegister,
      lenderRegister,
      lendToken = boxToken,
      id = inputBox.getId)
  }

  def this(inputBox: InputBox) = this(
    value = inputBox.getValue,
    fundingInfoRegister = new FundingInfoRegister(inputBox.getRegisters.get(0).getValue.asInstanceOf[Array[Long]]),
    lendingProjectDetailsRegister = new LendingProjectDetailsRegister(inputBox.getRegisters.get(1).getValue.asInstanceOf[Array[Coll[Byte]]]),
    singleLenderRegister = new SingleLenderRegister(inputBox.getRegisters.get(2).getValue.asInstanceOf[Array[Byte]]),
    lendToken = inputBox.getTokens.get(0),
    id = inputBox.getId
  )

  def funded(lenderAddress: String): SingleLenderLendingBox = {
    val lenderRegister = new SingleLenderRegister(lenderAddress)
    new SingleLenderLendingBox(
      value = fundingInfoRegister.fundingGoal,
      fundingInfoRegister,
      lendingProjectDetailsRegister,
      lenderRegister)
  }

  /**
   * Empty/Not Funded State
   * @param ctx
   * @param txB
   * @return
   */
  def getInitiationOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    val lendBoxContract = getLendBoxCreationContract(ctx)
    val lendBox = txB.outBoxBuilder()
      .value(Configs.fee * 2)
      .contract(lendBoxContract)
      .tokens(lendToken)
      .registers(fundingInfoRegister.toRegister, lendingProjectDetailsRegister.toRegister, singleLenderRegister.toRegister).build()

    lendBox
  }

  /**
   * Funded state
   * @param ctx
   * @param txB
   * @return
   */
  override def getFundedOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    // If lender is null, then it can't possibly be funded.
    if (singleLenderRegister.lendersAddress.getBytes().deep == Array.emptyByteArray.deep) {
      //@todo create better exception
      throw new Exception("Lender address is empty, but single lender box is funded.")
    }

    val lendBoxContract = getLendBoxCreationContract(ctx)
    val lendBox = txB.outBoxBuilder()
      .value(fundingInfoRegister.fundingGoal)
      .contract(lendBoxContract)
      .tokens(lendToken)
      .registers(fundingInfoRegister.toRegister, lendingProjectDetailsRegister.toRegister, singleLenderRegister.toRegister).build()

    lendBox
  }

  def getBorrowersAddress: ErgoAddress = {
    Address.create(lendingProjectDetailsRegister.borrowersPubKey).getErgoAddress
  }

  //  def fundedOutBox(ctx: BlockchainContext): OutBox
  //  def fundLendBox(pubkey: String, fundedAmount: Long): OutBox
  //  def addLender(pubkey: String)

  def getLendBoxCreationContract(ctx: BlockchainContext): ErgoContract = {
    ctx.compileContract(ConstantsBuilder.create()
      .item("fee", Configs.fee)
      .build(), singleLenderLendingBoxScript)
  }

  override def getLendersAddress: ErgoAddress = {
    Address.create(singleLenderRegister.lendersAddress).getErgoAddress
  }
}

object SingleLenderLendingBox {
  def createViaPaymentBox(paymentBox: SingleLenderInitiationPaymentBox): SingleLenderLendingBox = {
    val lendBoxInitialValue = paymentBox.value - Parameters.MinFee
    return new SingleLenderLendingBox(
      lendBoxInitialValue,
      paymentBox.fundingInfoRegister,
      paymentBox.lendingProjectDetailsRegister,
      SingleLenderRegister.emptyRegister)
  }
}

trait Box {
}

abstract class LendingBox extends Box {
  def getBorrowersAddress: ErgoAddress
  def getLendersAddress: ErgoAddress
  def getInitiationOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox
  def getFundedOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox
}