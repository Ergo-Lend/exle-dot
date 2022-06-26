package SLTokens.boxes

import SLTokens.contracts.{SLTCreateLendBoxContract, SLTFundLendBoxContract}
import boxes.Box
import org.ergoplatform.appkit.InputBox

class SLTCreateLendBox(
  inputBox: InputBox,
  contractData: SLTCreateLendBoxContract
) extends Box(inputBox)
