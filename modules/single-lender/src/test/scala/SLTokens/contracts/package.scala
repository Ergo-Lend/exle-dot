package SLTokens

import commons.configs.Tokens
import org.ergoplatform.appkit.Address
import tokens.SigUSD

package object contracts {
  val interestRate: Long = 100L // 10%
  val repaymentHeightLength: Long = 100L
  val deadlineHeightLength: Long = 100L
  val goal: Long = 100L
  val loanName: String = "Test Loan"
  val loanDescription: String = "Test Loan Description"
  val loanToken: Array[Byte] = SigUSD.id.getBytes
  val dummyAddress: Address = Address.create("4MQyML64GnzMxZgm")
  val lenderAddress: Address = Address.create("2fp75qcgMrTNR2vuLhiJYQt")
  val hackerAddress: Address = Address.create("m3iBKr65o53izn")
}
