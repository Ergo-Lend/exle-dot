package features.lend.boxes

import config.Configs
import features.lend.boxes.registers.{FundingInfoRegister, LendingProjectDetailsRegister}
import features.lend.contracts.lendingBoxScript
import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit._
import special.collection.Coll

/**
 * Depending on what we're doing with it, we may need to add lender tree
 *
 * @param box
 * @param fundingInfoRegister
 * @param lendingProjectDetailsRegister
 */
case class LendingBox(fundingInfoRegister: FundingInfoRegister,
                      lendingProjectDetailsRegister: LendingProjectDetailsRegister) extends Box {
  def apply(inputBox: InputBox): LendingBox = {
    val r4 = inputBox.getRegisters.get(0).getValue.asInstanceOf[Array[Long]]
    val r5 = inputBox.getRegisters.get(1).getValue.asInstanceOf[Array[Coll[Byte]]]
    val fundingInfoRegister = new FundingInfoRegister(r4)
    val lendingProjectDetailsRegister = LendingProjectDetailsRegister(r5)
    new LendingBox(fundingInfoRegister, lendingProjectDetailsRegister)
  }

  def getOutputBox(ctx: BlockchainContext): OutBox = {
    val txB = ctx.newTxBuilder()
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
      .build(), lendingBoxScript)
  }
}

trait Box {
  def getOutputBox(ctx: BlockchainContext): OutBox
}
