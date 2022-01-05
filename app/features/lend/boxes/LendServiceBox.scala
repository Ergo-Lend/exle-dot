package features.lend.boxes

import boxes.registers.RegisterTypes.{CollByteRegister, LongRegister, NumberRegister, StringRegister}
import config.Configs
import ergotools.LendServiceTokens
import features.lend.contracts.singleLenderLendServiceBoxScript
import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit.{Address, BlockchainContext, ConstantsBuilder, ErgoContract, ErgoContracts, ErgoId, ErgoToken, InputBox, OutBox, Parameters, UnsignedTransaction, UnsignedTransactionBuilder}

import scala.collection.JavaConverters.seqAsJavaListConverter

/**
 * ServiceBox
 * This is used for the accounting of the lend boxes.
 * Every creation of lending box the service box provides
 * a token to give identity to the lending box & repayment box.
 * The destruction of repayment box returns the lending token to
 * the service box.
 *
 * Lastly, it returns ErgoLends share of the interest back to the
 * ErgoLend's team wallet
 *
 * Tokens:
 * LendServiceNFT - 1
 * LendBoxToken - Infinite
 *
 * R4: ErgoLendPubKey
 * R5: ProfitSharing %
 */
class LendServiceBox(val value: Long,
                     val lendTokenAmount: Long,
                     val repaymentTokenAmount: Long,
                     val ergoLendPubKey: StringRegister,
                     override val profitSharingPercentage: NumberRegister)
  extends ServiceBox(ergoLendPubKey, profitSharingPercentage)
{
  override val nft: ErgoId = LendServiceTokens.nft
  val lendToken: ErgoId = LendServiceTokens.lendToken
  val repaymentToken: ErgoId = LendServiceTokens.repaymentToken
  override val serviceOwner: Address = Configs.serviceOwner

  def this(inputBox: InputBox) = this(
    value = inputBox.getValue,
    lendTokenAmount = inputBox.getTokens.get(1).getValue,
    repaymentTokenAmount = inputBox.getTokens.get(2).getValue,
    ergoLendPubKey = new StringRegister(inputBox.getRegisters.get(0).getValue.asInstanceOf[String]),
    profitSharingPercentage = new NumberRegister(inputBox.getRegisters.get(1).getValue.asInstanceOf[Long])
  )

  def generateServiceBoxTx(
                          serviceNFTBox: InputBox,
                          tokensBox: InputBox,
                          ctx: BlockchainContext,
                          txB: UnsignedTransactionBuilder
                          ): UnsignedTransaction = {
    val serviceBox = txB.outBoxBuilder()
      .value(value)
      .tokens(
        serviceNFTBox.getTokens.get(0),
        tokensBox.getTokens.get(0))
      .registers(
        ergoLendPubKey.toRegister,
        profitSharingPercentage.toRegister
      )
      .contract(getServiceBoxContract(ctx))
      .build()

    val boxesToSpend = Seq(serviceNFTBox, tokensBox)
    val ergoLendAddress = Address.create(ergoLendPubKey.value)
    val tx = txB
      .boxesToSpend(boxesToSpend.asJava)
      .outputs(serviceBox)
      .fee(Parameters.MinFee)
      .sendChangeTo(ergoLendAddress.getErgoAddress)
      .build()

    tx
  }

  override def getPubKeyAddress: ErgoAddress = {
    Address.create(ergoLendPubKey.value).getErgoAddress
  }

  override def getOutputServiceBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    val serviceBoxContract = getServiceBoxContract(ctx)

    val lendServiceNft = new ErgoToken(LendServiceTokens.nft, 1)
    val lendServiceTokens = new ErgoToken(LendServiceTokens.lendToken, lendTokenAmount)
    val repaymentServiceTokens = new ErgoToken(LendServiceTokens.repaymentToken, repaymentTokenAmount)
    val outputServiceBox = txB.outBoxBuilder()
      .value(value)
      .contract(serviceBoxContract)
      .tokens(
        lendServiceNft,
        lendServiceTokens,
        repaymentServiceTokens
      )
      .registers(
        ergoLendPubKey.toRegister,
        profitSharingPercentage.toRegister
      )
      .build()

    outputServiceBox
  }

  /**
   *
   * @param amount
   * @param ctx
   * @param txB
   * @return
   */
  def getOwnerProfitSharingBox(amountForProfitSplit: Long, txB: UnsignedTransactionBuilder): OutBox = {
    val profitSharePercentage = amountForProfitSplit * (profitSharingPercentage.value / 100)
    val ownerProfitSharingBox = new FundsToAddressBox(profitSharePercentage, serviceOwner.getErgoAddress)

    ownerProfitSharingBox.getOutputBox(txB)
  }

  def negateLendToken(): LendServiceBox = {
    return new LendServiceBox(
      value = value,
      lendTokenAmount = lendTokenAmount - 1,
      repaymentTokenAmount = repaymentTokenAmount,
      ergoLendPubKey = ergoLendPubKey,
      profitSharingPercentage = profitSharingPercentage)
  }

  def incrementLendToken(): LendServiceBox = {
    new LendServiceBox(
      value = value,
      lendTokenAmount = lendTokenAmount + 1,
      repaymentTokenAmount = repaymentTokenAmount,
      ergoLendPubKey = ergoLendPubKey,
      profitSharingPercentage = profitSharingPercentage)
  }

  def negateRepaymentToken(): LendServiceBox = {
    return new LendServiceBox(
      value = value,
      lendTokenAmount = lendTokenAmount,
      repaymentTokenAmount = repaymentTokenAmount - 1,
      ergoLendPubKey = ergoLendPubKey,
      profitSharingPercentage = profitSharingPercentage)
  }

  def incrementRepaymentToken(): LendServiceBox = {
    return new LendServiceBox(
      value = value,
      lendTokenAmount = lendTokenAmount,
      repaymentTokenAmount = repaymentTokenAmount + 1,
      ergoLendPubKey = ergoLendPubKey,
      profitSharingPercentage = profitSharingPercentage)
  }

  def exchangeLendRepaymentToken(): LendServiceBox = {
    return new LendServiceBox(
      value = value,
      lendTokenAmount = lendTokenAmount + 1,
      repaymentTokenAmount = repaymentTokenAmount - 1,
      ergoLendPubKey = ergoLendPubKey,
      profitSharingPercentage = profitSharingPercentage)
  }

  /**
   * Returns a list of outputServiceBox, profitSharing
   * @param repaymentBox
   * @param ctx
   * @param txB
   * @return
   */
  def consumeRepaymentBox(repaymentBox: RepaymentBox, ctx: BlockchainContext, txB: UnsignedTransactionBuilder): List[OutBox] = {
    val incrementedServiceBox = this.incrementRepaymentToken().getOutputServiceBox(ctx, txB)
    val profitSharingBox = this.getOwnerProfitSharingBox(repaymentBox.getRepaymentInterest, txB)

    val outputBoxList = List(incrementedServiceBox, profitSharingBox)

    outputBoxList
  }

  def fundedLend(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    val exchangedServiceBox = this.exchangeLendRepaymentToken().getOutputServiceBox(ctx, txB)

    exchangedServiceBox
  }

  def createLend(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    val decrementedLendServiceBox = this.negateLendToken().getOutputServiceBox(ctx, txB)

    decrementedLendServiceBox
  }

  def refundLend(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox = {
    val incrementedLendServiceBox = this.incrementLendToken().getOutputServiceBox(ctx, txB)

    incrementedLendServiceBox
  }

  override def getServiceBoxContract(ctx: BlockchainContext): ErgoContract = {
    ctx.compileContract(ConstantsBuilder.create()
      .item("ownerPk", serviceOwner.getPublicKey)
      .item("serviceNFT", nft)
      .item("serviceToken", lendToken)
      .build(), singleLenderLendServiceBoxScript)
  }
}

/**
 *
 * @param serviceToken
 * @param boxToken
 */
abstract class ServiceBox(val pubKey: CollByteRegister,
                           val profitSharingPercentage: LongRegister
                         ) extends Box {
  val nft: ErgoId
  val serviceOwner: Address

  def getOutputServiceBox(ctx: BlockchainContext, txB: UnsignedTransactionBuilder): OutBox
  def getPubKeyAddress: ErgoAddress
  def getServiceBoxContract(ctx: BlockchainContext): ErgoContract
}
