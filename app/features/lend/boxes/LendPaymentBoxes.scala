package features.lend.boxes

import features.lend.boxes.registers.{FundingInfoRegister, LendingProjectDetailsRegister, SingleAddressRegister, SingleLenderRegister}
import org.ergoplatform.appkit.InputBox
import special.collection.Coll

trait PaymentBox

class SingleLenderInitiationPaymentBox(val value: Long,
                                       val fundingInfoRegister: FundingInfoRegister,
                                       val lendingProjectDetailsRegister: LendingProjectDetailsRegister) extends PaymentBox {


  def apply(inputBox: InputBox): SingleLenderInitiationPaymentBox = {
    val r4 = inputBox.getRegisters.get(0).getValue.asInstanceOf[Array[Long]]
    val r5 = inputBox.getRegisters.get(1).getValue.asInstanceOf[Array[Coll[Byte]]]
    val fundingInfoRegister = new FundingInfoRegister(r4)
    val lendingProjectDetailsRegister = new LendingProjectDetailsRegister(r5)

    new SingleLenderInitiationPaymentBox(
      inputBox.getValue,
      fundingInfoRegister,
      lendingProjectDetailsRegister)
  }

  def this(inputBox: InputBox) = this(
    value = inputBox.getValue,
    fundingInfoRegister = new FundingInfoRegister(inputBox.getRegisters.get(0).getValue.asInstanceOf[Array[Long]]),
    lendingProjectDetailsRegister = new LendingProjectDetailsRegister(inputBox.getRegisters.get(0).getValue.asInstanceOf[Array[Coll[Byte]]])
  )
}

class SingleLenderFundLendPaymentBox(val value: Long,
                                     val singleLenderRegister: SingleLenderRegister) extends PaymentBox {
  def this(inputBox: InputBox) = this(
    value = inputBox.getValue,
    singleLenderRegister = new SingleLenderRegister(inputBox.getRegisters.get(0).getValue.asInstanceOf[Array[Byte]])
  )
}

class SingleLenderFundRepaymentPaymentBox(val value: Long,
                                     val singleAddressRegister: SingleAddressRegister) extends PaymentBox {
  def this(inputBox: InputBox) = this(
    value = inputBox.getValue,
    singleAddressRegister = new SingleAddressRegister(inputBox.getRegisters.get(0).getValue.asInstanceOf[Array[Byte]])
  )
}
