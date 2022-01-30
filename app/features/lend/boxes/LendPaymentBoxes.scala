package features.lend.boxes

import features.lend.boxes.registers.{BorrowerRegister, FundingInfoRegister, LendingProjectDetailsRegister, SingleAddressRegister, SingleLenderRegister}
import org.ergoplatform.appkit.InputBox
import special.collection.Coll

trait PaymentBox

class SingleLenderInitiationPaymentBox(val value: Long,
                                       val fundingInfoRegister: FundingInfoRegister,
                                       val lendingProjectDetailsRegister: LendingProjectDetailsRegister,
                                       val borrowerRegister: BorrowerRegister) extends PaymentBox {


  def this(inputBox: InputBox,
           fundingInfoRegister: FundingInfoRegister,
           lendingProjectDetailsRegister: LendingProjectDetailsRegister,
           borrowerRegister: BorrowerRegister) = this(
    value = inputBox.getValue,
    fundingInfoRegister = fundingInfoRegister,
    lendingProjectDetailsRegister = lendingProjectDetailsRegister,
    borrowerRegister = borrowerRegister
  )
}

class SingleLenderFundLendPaymentBox(val value: Long,
                                     val singleLenderRegister: SingleLenderRegister) extends PaymentBox {
  def this(inputBox: InputBox, singleLenderRegister: SingleLenderRegister) = this(
    value = inputBox.getValue,
    singleLenderRegister = singleLenderRegister
  )
}

class SingleLenderFundRepaymentPaymentBox(val value: Long,
                                     val singleAddressRegister: SingleAddressRegister) extends PaymentBox {
  def this(inputBox: InputBox, singleAddressRegister: SingleAddressRegister) = this(
    value = inputBox.getValue,
    singleAddressRegister = singleAddressRegister
  )
}
