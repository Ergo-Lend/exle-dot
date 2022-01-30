package features.lend.boxes

import boxes.registers.RegisterTypes.{CollByteRegister, LongRegister, NumberRegister, StringRegister}
import config.Configs
import ergotools.LendServiceTokens
import features.lend.boxes.registers.{CreationInfoRegister, ProfitSharingRegister, ServiceBoxInfoRegister, SingleAddressRegister}
import features.lend.contracts.singleLenderLendServiceBoxScript
import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit.{Address, BlockchainContext, ConstantsBuilder, ErgoContract, ErgoContracts, ErgoId, ErgoToken, InputBox, OutBox, Parameters, UnsignedTransaction, UnsignedTransactionBuilder}
import special.collection.Coll

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
 *
 * R4: Coll[Long] -> Creation height, Version
 * R5: Coll[Coll[Byte]] -> ErgoLend, SingleLender allows one lender to trade
 * R6: Coll[Byte] -> SingleLenderServiceBox
 * R7: Coll[Byte] -> OwnerAddress
 * R8: Coll[Long] -> ProfitSharing Percentage
 */
class LendServiceBox(val value: Long,
                     val lendTokenAmount: Long,
                     val repaymentTokenAmount: Long,
                     val creationInfo: CreationInfoRegister,
                     val serviceInfo: ServiceBoxInfoRegister,
                     val boxInfo: StringRegister,
                     val ergoLendPubKey: SingleAddressRegister,
                     override val profitSharingPercentage: ProfitSharingRegister)
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
    creationInfo = new CreationInfoRegister(inputBox.getRegisters.get(0).getValue.asInstanceOf[Coll[Long]].toArray),
    serviceInfo = new ServiceBoxInfoRegister(inputBox.getRegisters.get(1).getValue.asInstanceOf[Coll[Coll[Byte]]].toArray),
    boxInfo = new StringRegister(inputBox.getRegisters.get(2).getValue.asInstanceOf[Coll[Byte]]),
    ergoLendPubKey = new SingleAddressRegister(inputBox.getRegisters.get(3).getValue.asInstanceOf[Coll[Byte]].toArray),
    profitSharingPercentage = new ProfitSharingRegister(inputBox.getRegisters.get(4).getValue.asInstanceOf[Coll[Long]].toArray)
  )

  override def getPubKeyAddress: ErgoAddress = {
    Address.create(ergoLendPubKey.address).getErgoAddress
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
        creationInfo.toRegister,
        serviceInfo.toRegister,
        boxInfo.toRegister,
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
  def getOwnerProfitSharingBox(amountForProfitSplit: Long,
                               ctx: BlockchainContext,
                               txB: UnsignedTransactionBuilder): OutBox = {
    val profitSharePercentage = amountForProfitSplit * (profitSharingPercentage.profitSharingPercentage / 100)
    val ownerProfitSharingBox = new FundsToAddressBox(profitSharePercentage, ergoLendPubKey.address)

    ownerProfitSharingBox.getOutputBox(ctx, txB)
  }

  def negateLendToken(): LendServiceBox = {
    return new LendServiceBox(
      value = value,
      lendTokenAmount = lendTokenAmount - 1,
      repaymentTokenAmount = repaymentTokenAmount,
      creationInfo = creationInfo,
      serviceInfo = serviceInfo,
      boxInfo = boxInfo,
      ergoLendPubKey = ergoLendPubKey,
      profitSharingPercentage = profitSharingPercentage)
  }

  def incrementLendToken(): LendServiceBox = {
    new LendServiceBox(
      value = value,
      lendTokenAmount = lendTokenAmount + 1,
      repaymentTokenAmount = repaymentTokenAmount,
      creationInfo = creationInfo,
      serviceInfo = serviceInfo,
      boxInfo = boxInfo,
      ergoLendPubKey = ergoLendPubKey,
      profitSharingPercentage = profitSharingPercentage)
  }

  def negateRepaymentToken(): LendServiceBox = {
    return new LendServiceBox(
      value = value,
      lendTokenAmount = lendTokenAmount,
      repaymentTokenAmount = repaymentTokenAmount - 1,
      creationInfo = creationInfo,
      serviceInfo = serviceInfo,
      boxInfo = boxInfo,
      ergoLendPubKey = ergoLendPubKey,
      profitSharingPercentage = profitSharingPercentage)
  }

  def incrementRepaymentToken(): LendServiceBox = {
    return new LendServiceBox(
      value = value,
      lendTokenAmount = lendTokenAmount,
      repaymentTokenAmount = repaymentTokenAmount + 1,
      creationInfo = creationInfo,
      serviceInfo = serviceInfo,
      boxInfo = boxInfo,
      ergoLendPubKey = ergoLendPubKey,
      profitSharingPercentage = profitSharingPercentage)
  }

  def exchangeLendRepaymentToken(): LendServiceBox = {
    return new LendServiceBox(
      value = value,
      lendTokenAmount = lendTokenAmount + 1,
      repaymentTokenAmount = repaymentTokenAmount - 1,
      creationInfo = creationInfo,
      serviceInfo = serviceInfo,
      boxInfo = boxInfo,
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
    val profitSharingBox = this.getOwnerProfitSharingBox(repaymentBox.getRepaymentInterest, ctx, txB)

    val outputBoxList = List(incrementedServiceBox, profitSharingBox)

    outputBoxList
  }

  def fundedLend(): LendServiceBox = {
    val exchangedServiceBox = this.exchangeLendRepaymentToken()

    exchangedServiceBox
  }

  def createLend(): LendServiceBox = {
    val decrementedLendServiceBox = this.negateLendToken()

    decrementedLendServiceBox
  }

  def refundLend(): LendServiceBox = {
    val incrementedLendServiceBox = this.incrementLendToken()

    incrementedLendServiceBox
  }

  override def getServiceBoxContract(ctx: BlockchainContext): ErgoContract = {
    ctx.compileContract(ConstantsBuilder.create()
      .item("ownerPk", serviceOwner.getPublicKey)
      .item("serviceNFT", LendServiceTokens.nft.getBytes)
      .item("serviceLendToken", LendServiceTokens.lendToken.getBytes)
      .item("serviceRepaymentToken", LendServiceTokens.repaymentToken.getBytes)
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
