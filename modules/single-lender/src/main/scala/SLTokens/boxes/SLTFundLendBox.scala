package SLTokens.boxes

import SLTokens.contracts.SLTFundLendBoxContract
import boxes.Box
import org.ergoplatform.appkit.InputBox
import registers.RegVal

case class SLTFundLendBox(inputBox: InputBox, contractData: SLTFundLendBoxContract) extends Box(inputBox)
