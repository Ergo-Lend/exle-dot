package SLTokens.boxes

import SLTokens.contracts.{SLTFundLendBoxContract, SLTFundRepaymentBoxContract}
import boxes.Box
import org.ergoplatform.appkit.InputBox

class SLTFundRepaymentBox(
  inputBox: InputBox,
  contractData: SLTFundRepaymentBoxContract
) extends Box(inputBox)
