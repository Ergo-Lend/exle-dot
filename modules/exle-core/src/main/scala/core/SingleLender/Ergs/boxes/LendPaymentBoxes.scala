package core.SingleLender.Ergs.boxes

import core.SingleLender.Ergs.boxes.registers.{BorrowerRegister, FundingInfoRegister, LendingProjectDetailsRegister, SingleAddressRegister, SingleLenderRegister}
import org.ergoplatform.appkit.InputBox
import special.collection.Coll

import scala.collection.mutable

trait PaymentBox

class SingleLenderInitiationPaymentBox(val value: Long,
                                       val fundingInfoRegister: FundingInfoRegister,
                                       val lendingProjectDetailsRegister: LendingProjectDetailsRegister,
                                       val borrowerRegister: BorrowerRegister) extends PaymentBox {


  def this(inputBoxes: mutable.Buffer[InputBox],
           fundingInfoRegister: FundingInfoRegister,
           lendingProjectDetailsRegister: LendingProjectDetailsRegister,
           borrowerRegister: BorrowerRegister) = this(
    value = inputBoxes.foldLeft(0L)((sum, box) => sum + box.getValue),
    fundingInfoRegister = fundingInfoRegister,
    lendingProjectDetailsRegister = lendingProjectDetailsRegister,
    borrowerRegister = borrowerRegister
  )
}

class SingleLenderFundLendPaymentBox(val value: Long,
                                     val singleLenderRegister: SingleLenderRegister) extends PaymentBox {
  def this(inputBoxes: mutable.Buffer[InputBox], singleLenderRegister: SingleLenderRegister) = this(
    value = inputBoxes.foldLeft(0L)((sum, box) => sum + box.getValue),
    singleLenderRegister = singleLenderRegister
  )
}

class SingleLenderFundRepaymentPaymentBox(val value: Long,
                                     val singleAddressRegister: SingleAddressRegister) extends PaymentBox {
  def this(inputBoxes: mutable.Buffer[InputBox], singleAddressRegister: SingleAddressRegister) = this(
    value = inputBoxes.foldLeft(0L)((sum, box) => sum + box.getValue),
    singleAddressRegister = singleAddressRegister
  )
}
