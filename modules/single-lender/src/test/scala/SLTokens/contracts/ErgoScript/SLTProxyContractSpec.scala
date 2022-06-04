package SLTokens.contracts.ErgoScript

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SLTProxyContractSpec extends AnyWordSpec with Matchers {
  "SLT CreateLendBox ProxyContract" when {
    "Creating" should {
      "Not be hacked by others into their account" in {}

      "Ensure the box created details are correct" in {}
    }
  }

  "SLT FundLendBox ProxyContract" when {}

  "SLT FundRepaymentBox ProxyContract" when {}
}
