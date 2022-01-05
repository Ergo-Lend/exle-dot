package features.lend.boxes

import config.Configs
import ergotools.LendServiceTokens
import errors.failedTxException
import features.lend.boxes.registers.{FundingInfoRegister, LenderRegister, LendingProjectDetailsRegister, RepaymentDetailsRegister, SingleLenderRegister}
import features.lend.contracts.singleLenderRepaymentBoxScript
import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit.{Address, BlockchainContext, ConstantsBuilder, ErgoContract, ErgoId, ErgoToken, InputBox, OutBox, Parameters, UnsignedTransactionBuilder}
import special.collection.Coll

/**
 * RepaymentBox: Single Lender
 * In a SingleLenderRepaymentBox there are 3 states
 * 1. Empty - 0 value
 * 2. Partially Funded - Value less than completed value
 * 3. Fully Funded
 *
 * This repayment box consists of only a single lender.
 * The repayment box can be funded incrementally till it reaches it's repayment goal.
 *
 *
 * @param fundingInfoRegister
 * @param lendingProjectDetailsRegister
 * @param singleLenderRegister
 * @param repaymentDetailsRegister
 */
class SingleLenderRepaymentBox(
                              val value: Long = 0,
                              fundingInfoRegister: FundingInfoRegister,
                              lendingProjectDetailsRegister: LendingProjectDetailsRegister,
                              val singleLenderRegister: SingleLenderRegister,
                              repaymentDetailsRegister: RepaymentDetailsRegister,
                              val repaymentToken: ErgoToken = new ErgoToken(LendServiceTokens.repaymentToken, 1),
                              val id: ErgoId = ErgoId.create("")
                              ) extends RepaymentBox(fundingInfoRegister, lendingProjectDetailsRegister, singleLenderRegister, repaymentDetailsRegister) {
  val repaymentBoxToken = new ErgoToken(LendServiceTokens.repaymentToken, 1)
  def this(inputBox: InputBox) = this (
    value = inputBox.getValue,
    fundingInfoRegister = new FundingInfoRegister(inputBox.getRegisters.get(0).getValue.asInstanceOf[Array[Long]]),
    lendingProjectDetailsRegister = new LendingProjectDetailsRegister(inputBox.getRegisters.get(1).getValue.asInstanceOf[Array[Coll[Byte]]]),
    singleLenderRegister = new SingleLenderRegister(inputBox.getRegisters.get(2).getValue.asInstanceOf[Array[Byte]]),
    repaymentDetailsRegister = new RepaymentDetailsRegister(
      inputBox.getRegisters.get(3).getValue.asInstanceOf[Array[Long]]),
    repaymentToken = inputBox.getTokens.get(0),
    id = inputBox.getId
  )

  /**
   * Returns the outputbox for repayment
   *
   * if the box value is 0, it means it is instantiated. Therefore we return an empty repayment box
   * if the box value is < repaymentAmount, we return a partially fund box
   * if the box value is >= repaymentAmount, we return the box with the value
   *
   * as long as the repayment is funded, it doesn't matter whether it is overfunded, we still repay the lender
   * whatever amount it has in it.
   *
   * @param ctx
   * @param txB
   * @return
   */
  override def getOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    var boxValue = value
    if (boxValue == 0L) {
      boxValue = Configs.fee * 2
    }

    //@todo add tokens
    val repaymentBoxContract = getRepaymentContract(ctx)
    val repaymentBox = txB.outBoxBuilder()
      .value(boxValue)
      .contract(repaymentBoxContract)
      .tokens(
        repaymentToken
      )
      .registers(
        fundingInfoRegister.toRegister,
        lendingProjectDetailsRegister.toRegister,
        singleLenderRegister.toRegister,
        repaymentDetailsRegister.toRegister)
      .build()

    repaymentBox
  }

  /**
   * When we fund a box, we give them a new SingleLenderRepaymentBox
   * @param fundingAmount
   * @return
   */
  override def fundBox(fundingAmount: Long): SingleLenderRepaymentBox = {
    new SingleLenderRepaymentBox(
      value = value + fundingAmount,
      fundingInfoRegister = fundingInfoRegister,
      lendingProjectDetailsRegister = lendingProjectDetailsRegister,
      singleLenderRegister = singleLenderRegister,
      repaymentDetailsRegister = repaymentDetailsRegister,
      repaymentToken = repaymentToken,
    )
  }

  override def fundedBox(): SingleLenderRepaymentBox = {
    new SingleLenderRepaymentBox(
      value = repaymentDetailsRegister.repaymentAmount - Parameters.MinFee,
      fundingInfoRegister = fundingInfoRegister,
      lendingProjectDetailsRegister = lendingProjectDetailsRegister,
      singleLenderRegister = singleLenderRegister,
      repaymentDetailsRegister = repaymentDetailsRegister,
      repaymentToken = repaymentToken
    )
  }

  def repaidLendersPaymentBox(ergoLendInterest: Long): FundsToAddressBox = {
    val isFunded = value >= repaymentDetailsRegister.repaymentAmount
    if (isFunded) {
      new FundsToAddressBox(value - ergoLendInterest - Parameters.MinFee, singleLenderRegister.lendersAddress)
    } else {
      // @todo Better failure
      throw failedTxException(s"repayment not fully repaid")
    }
  }

  override def getLendersAddress: ErgoAddress = {
    Address.create(singleLenderRegister.toString).getErgoAddress
  }

  override def getRepaymentInterest: Long = {
    repaymentDetailsRegister.totalInterestAmount
  }

  def getRepaymentContract(ctx: BlockchainContext): ErgoContract = {
    ctx.compileContract(ConstantsBuilder.create()
    .item("fee", Configs.fee)
    .build(), singleLenderRepaymentBoxScript)
  }
}

abstract class RepaymentBox(
                    val fundingInfoRegister: FundingInfoRegister,
                    val lendingProjectDetailsRegister: LendingProjectDetailsRegister,
                    val lenderRegister: LenderRegister,
                    val repaymentDetailsRegister: RepaymentDetailsRegister
                  ) extends Box {
  def getLendersAddress: ErgoAddress

  def fundBox(fundingAmount: Long): RepaymentBox
  def fundedBox(): RepaymentBox
  def getOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox
  def getRepaymentInterest: Long
}