package contracts

import boxes.registers.RegisterTypes.StringRegister
import config.Configs
import features.lend.boxes.LendServiceBox
import features.lend.boxes.registers.{CreationInfoRegister, ProfitSharingRegister, ServiceBoxInfoRegister, SingleAddressRegister}
import org.ergoplatform.appkit.{Address, Parameters}

/**
 * We need
 * 1. Service Boxes
 * 2. Lend Boxes
 * 3. Repayment Boxes
 */
package object SingleLender {
  def buildGenesisServiceBox(): LendServiceBox = {
    val creationInfo = new CreationInfoRegister(creationHeight = 1L)
    val serviceInfo = new ServiceBoxInfoRegister(name = "LendBox", description = "Testing")
    val boxInfo = new StringRegister("SingleLenderServiceBox")
    val ownerPubKey = new SingleAddressRegister(Configs.serviceOwner.toString)
    val profitSharingRegister = new ProfitSharingRegister(Configs.profitSharingPercentage, Configs.serviceFee)
    val lendServiceBox = new LendServiceBox(
      value = Parameters.MinFee,
      lendTokenAmount = 100,
      repaymentTokenAmount = 100,
      creationInfo = creationInfo,
      serviceInfo = serviceInfo,
      boxInfo = boxInfo,
      ergoLendPubKey = ownerPubKey,
      profitSharingPercentage = profitSharingRegister
    )

    lendServiceBox
  }
}
