package SLTokens.boxes

import SLTokens.SLTTokens
import SLTokens.contracts.SLTRepaymentBoxContract
import boxes.{Box, BoxWrapper, FundsToAddressBox}
import commons.boxes.registers.RegisterTypes.CollByteRegister
import commons.configs.Tokens
import commons.ergo.ErgCommons
import commons.errors.IncompatibleTokenException
import commons.registers.{BorrowerRegister, FundingInfoRegister, LendingProjectDetailsRegister, RepaymentDetailsRegisterV2, SingleLenderRegister}
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoContract, ErgoId, ErgoToken, InputBox, OutBox, Parameters, UnsignedTransactionBuilder}
import special.collection.Coll
import tokens.SigUSD

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

case class SLTRepaymentBox(
  value: Long,
  fundingInfoRegister: FundingInfoRegister,
  lendingProjectDetailsRegister: LendingProjectDetailsRegister,
  borrowerRegister: BorrowerRegister,
  loanTokenIdRegister: CollByteRegister,
  singleLenderRegister: SingleLenderRegister,
  repaymentDetailsRegister: RepaymentDetailsRegisterV2,
  override val id: ErgoId = ErgoId.create(""),
  override val tokens: Seq[ErgoToken] = Seq(
    new ErgoToken(SLTTokens.repaymentTokenId, 1)
  ),
  override val box: Option[Box] = Option(null)
) extends BoxWrapper {
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
    loanTokenIdRegister = new CollByteRegister(
      inputBox.getRegisters.get(3).getValue.asInstanceOf[Coll[Byte]].toArray
    ),
    singleLenderRegister = new SingleLenderRegister(
      inputBox.getRegisters.get(4).getValue.asInstanceOf[Coll[Byte]].toArray
    ),
    repaymentDetailsRegister = new RepaymentDetailsRegisterV2(
      inputBox.getRegisters.get(5).getValue.asInstanceOf[Coll[Long]].toArray
    ),
    id = inputBox.getId,
    tokens = inputBox.getTokens.asScala.toSeq,
    box = Option(Box(inputBox))
  )

  def this(sltLendBox: SLTLendBox, fundedHeight: Long) = this(
    value = Parameters.MinFee,
    fundingInfoRegister = sltLendBox.fundingInfoRegister,
    lendingProjectDetailsRegister = sltLendBox.lendingProjectDetailsRegister,
    borrowerRegister = sltLendBox.borrowerRegister,
    loanTokenIdRegister = sltLendBox.loanTokenIdRegister,
    singleLenderRegister = sltLendBox.singleLenderRegister,
    repaymentDetailsRegister = RepaymentDetailsRegisterV2.apply(
      fundedHeight,
      sltLendBox.fundingInfoRegister
    )
  )

  override def getContract(ctx: BlockchainContext): ErgoContract =
    SLTRepaymentBoxContract.getContract(ctx).contract.ergoContract

  override def getOutBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder
  ): OutBox =
    txB
      .outBoxBuilder()
      .value(value)
      .contract(getContract(ctx))
      .registers(
        fundingInfoRegister.toRegister,
        lendingProjectDetailsRegister.toRegister,
        borrowerRegister.toRegister,
        loanTokenIdRegister.toRegister,
        singleLenderRegister.toRegister,
        repaymentDetailsRegister.toRegister
      )
      .tokens(tokens: _*)
      .build()
}

object SLTRepaymentBox {

  def fundBox(
    sltRepaymentBox: SLTRepaymentBox,
    amount: Long
  ): SLTRepaymentBox = {
    val zeroRepaid: Boolean = sltRepaymentBox.tokens.length == 1
    if (zeroRepaid) {
      sltRepaymentBox.copy(tokens =
        Seq(sltRepaymentBox.tokens.head, SigUSD(amount).toErgoToken)
      )
    } else {
      def incrementSigUSDValue(token: ErgoToken, amount: Long): ErgoToken =
        if (token.getId.toString.equals(Tokens.sigUSD)) {
          new ErgoToken(token.getId, token.getValue + amount)
        } else {
          token
        }
      val fundedTokenList: Seq[ErgoToken] =
        sltRepaymentBox.tokens.map(incrementSigUSDValue(_, amount))

      sltRepaymentBox.copy(tokens = fundedTokenList)
    }
  }

  def fundBox(
    sltRepaymentBox: SLTRepaymentBox,
    paymentBox: InputBox
  ): SLTRepaymentBox = {
    // @todo, check if there are token
    val token: ErgoToken = paymentBox.getTokens.asScala.toSeq.head
    if (!token.getId.getBytes.sameElements(
          sltRepaymentBox.loanTokenIdRegister.value
        )) {
      val receivedTokenId: ErgoId = new ErgoId(
        sltRepaymentBox.loanTokenIdRegister.value
      )
      throw IncompatibleTokenException(
        token.getId.toString,
        receivedTokenId.toString
      )
    }

    val tokenAmount: Long = token.getValue
    fundBox(sltRepaymentBox, tokenAmount)
  }

  def fromFundedLendBox(
    sltLendBox: SLTLendBox,
    fundedHeight: Long): SLTRepaymentBox = {
    new SLTRepaymentBox(sltLendBox, fundedHeight)
  }
}

object SLTRepaymentDistribution {
  /**
   * Increment the Repayment Register with the new funded value
   * and remove the sigUSD token
   * @param sltRepaymentBox slt repayment box
   * @return
   */
  def getOutRepaymentBox(sltRepaymentBox: SLTRepaymentBox): SLTRepaymentBox = {
    val loanTokenId: ErgoId = new ErgoId(sltRepaymentBox.loanTokenIdRegister.value)
    val sigUSDToken: ErgoToken = sltRepaymentBox.tokens.filter(_.getId.equals(loanTokenId)).head
    val totalRepaid: Long = sltRepaymentBox.repaymentDetailsRegister.repaymentPaid + sigUSDToken.getValue
    val valueLeft: Long = sltRepaymentBox.value - (ErgCommons.MinBoxFee * 3)
    val repaymentAddedSLTRepaymentBox: SLTRepaymentBox = sltRepaymentBox.copy(
      value = valueLeft,
      tokens = sltRepaymentBox.tokens.filter(!_.getId.equals(loanTokenId)),
      repaymentDetailsRegister = sltRepaymentBox.repaymentDetailsRegister.copy(
        repaymentPaid = totalRepaid)
    )

    repaymentAddedSLTRepaymentBox
  }

  def getFundsRepaidBox(sltRepaymentBox: SLTRepaymentBox, sltServiceBox: SLTServiceBox): Seq[FundsToAddressBox] = {
    val repaymentShare: Seq[ErgoToken] = calculateRepayment(
        sltRepaymentBox.tokens(1).getValue,
        sltRepaymentBox.fundingInfoRegister.interestRatePercent,
        sltServiceBox.profitSharingRegister.profitSharingPercentage)
      .map(
        new ErgoToken(sltRepaymentBox.loanTokenIdRegister.value, _))
    val fundsToLenderBox: FundsToAddressBox = FundsToAddressBox(
      address = Address.create(sltRepaymentBox.singleLenderRegister.address),
      tokens = Seq(repaymentShare.head))
    val fundsToProfitSharingBox: FundsToAddressBox = FundsToAddressBox(
      address = Address.create(sltServiceBox.exlePubKey.address),
      tokens = Seq(repaymentShare(1)))

    Seq(fundsToLenderBox, fundsToProfitSharingBox)
  }

  /**
   *
   * @param totalAmount Amount to be repaid
   * @param interestRatePercent Interest rate of the loan
   * @param profitSharingPercent Profit Sharing Percentage for owner
   * @return Seq[LendersShare, ProtocolOwnerShare]
   */
  def calculateRepayment(totalAmount: Long, interestRatePercent: Long, profitSharingPercent: Long, percentageDenominator: Long): Seq[Long] = {
    val interestAmount = (totalAmount * interestRatePercent) / percentageDenominator
    val protocolOwnerShare: Long = (interestAmount * profitSharingPercent) / percentageDenominator
    val lendersShare: Long = totalAmount - protocolOwnerShare

    Seq(lendersShare, protocolOwnerShare)
  }

  def calculateRepayment(totalAmount: Long, interestRatePercent: Long, profitSharingPercent: Long): Seq[Long] = {
    calculateRepayment(totalAmount, interestRatePercent, profitSharingPercent, 1000L)
  }
}
