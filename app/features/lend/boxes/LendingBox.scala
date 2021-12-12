package features.lend.boxes

import config.Configs
import features.lend.boxes.registers.{FundingInfoRegister, LenderRegister, LendingProjectDetailsRegister, SingleLenderRegister}
import features.lend.contracts.singleLenderLendingBoxScript
import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit._
import special.collection.Coll

/**
 * Depending on what we're doing with it, we may need to add lender tree
 *
 * For Single Lender Lending Box,
 * if the funds are
 * - Insufficient -> Proxy contract refunds currency
 * - Perfectly Funded -> Cool
 * - Over Funded -> difference between exceeded funds and fundingGoal is given to borrower as change without adding to repaymentGoal
 *
 * @param box
 * @param fundingInfoRegister
 * @param lendingProjectDetailsRegister
 */
case class SingleLenderLendingBox(value: Long,
                                  fundingInfoRegister: FundingInfoRegister,
                                  lendingProjectDetailsRegister: LendingProjectDetailsRegister,
                                  singleLenderRegister: SingleLenderRegister) extends LendingBox(value) {
  def apply(inputBox: InputBox): SingleLenderLendingBox = {
    val r4 = inputBox.getRegisters.get(0).getValue.asInstanceOf[Array[Long]]
    val r5 = inputBox.getRegisters.get(1).getValue.asInstanceOf[Array[Coll[Byte]]]
    val r6 = inputBox.getRegisters.get(2).getValue.asInstanceOf[Array[Byte]]
    val fundingInfoRegister = new FundingInfoRegister(r4)
    val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(r5)
    val lenderRegister = new SingleLenderRegister(r6)
    new SingleLenderLendingBox(
      inputBox.getValue,
      fundingInfoRegister,
      lendingProjectDetailsRegister,
      lenderRegister)
  }

  def getInitiationOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    val lendBoxContract = getLendBoxCreationContract(ctx)
    val lendBox = txB.outBoxBuilder()
      .value(Configs.fee * 2)
      .contract(lendBoxContract)
      .registers(fundingInfoRegister.toRegister, lendingProjectDetailsRegister.toRegister).build()

    lendBox
  }

  override def getFundingOutputBox(amount: Long, ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    val lendBoxContract = getLendBoxCreationContract(ctx)
    val lendBox = txB.outBoxBuilder()
      .value(Configs.fee * 2)
      .contract(lendBoxContract)
      .registers(fundingInfoRegister.toRegister, lendingProjectDetailsRegister.toRegister).build()

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
}

trait Box {
}

abstract class LendingBox(value: Long) extends Box {
  def getBorrowersAddress: ErgoAddress
  def getInitiationOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox
  def getFundingOutputBox(amount: Long, ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox
}