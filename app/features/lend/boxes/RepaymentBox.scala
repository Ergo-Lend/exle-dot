package features.lend.boxes

import config.Configs
import features.lend.boxes.registers.{FundingInfoRegister, LenderRegister, LendingProjectDetailsRegister, RepaymentDetailsRegister, SingleLenderRegister}
import features.lend.contracts.singleLenderRepaymentBoxScript
import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit.{Address, BlockchainContext, ConstantsBuilder, ErgoContract, OutBox, UnsignedTransactionBuilder}

/**
 * RepaymentBox: Single Lender
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
                              fundingInfoRegister: FundingInfoRegister,
                              lendingProjectDetailsRegister: LendingProjectDetailsRegister,
                              singleLenderRegister: SingleLenderRegister,
                              repaymentDetailsRegister: RepaymentDetailsRegister
                              ) extends RepaymentBox {

  override def getInitiationOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    val repaymentBoxContract = getRepaymentContract(ctx)
    val repaymentBox = txB.outBoxBuilder()
      .value(Configs.fee * 2)
      .contract(repaymentBoxContract)
      .registers(
        fundingInfoRegister.toRegister,
        lendingProjectDetailsRegister.toRegister,
        singleLenderRegister.toRegister,
        repaymentDetailsRegister.toRegister)
      .build()

    repaymentBox
  }

  /**
   * OutputBox: Funding RepaymentBox
   * When we are adding funds to the RepaymentBox, the funds can be added incrementally.
   *
   * @param ctx
   * @param txB
   * @return
   */
  override def getFundingOutputBox(fundingAmount: Long, ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    val repaymentBoxContract = getRepaymentContract(ctx)
    val repaymentBox = txB.outBoxBuilder()
      .value(Configs.fee * 2)
      .contract(repaymentBoxContract)
      .registers(
        fundingInfoRegister.toRegister,
        lendingProjectDetailsRegister.toRegister,
        singleLenderRegister.toRegister,
        repaymentDetailsRegister.toRegister)
      .build()

    repaymentBox
  }

  override def getLendersAddress: ErgoAddress = {
    Address.create(singleLenderRegister.toString).getErgoAddress
  }

  def getRepaymentContract(ctx: BlockchainContext): ErgoContract = {
    ctx.compileContract(ConstantsBuilder.create()
    .item("fee", Configs.fee)
    .build(), singleLenderRepaymentBoxScript)
  }
}

trait RepaymentBox extends Box {
  def getLendersAddress: ErgoAddress
  def getInitiationOutputBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox

  /**
   * OutputBox: Funding RepaymentBox
   * When we are adding funds to the RepaymentBox, the funds can be added incrementally.
   * @param ctx
   * @param txB
   * @return
   */
  def getFundingOutputBox(fundingAmount: Long, ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox
}