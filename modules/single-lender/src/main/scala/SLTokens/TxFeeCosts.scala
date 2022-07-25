package SLTokens

import commons.configs.ServiceConfig
import commons.ergo.ErgCommons

object TxFeeCosts {
  // CreationTxFee: ServiceFee + MinFee
  val creationTxFee: Long = ServiceConfig.serviceFee + ErgCommons.MinMinerFee
  val fundLendTxFee: Long = ErgCommons.MinMinerFee * 2

  def fundRepaymentTxFee(currentValue: Long = 0): Long =
    if (currentValue == 0) ErgCommons.MinMinerFee * 4
    else (ErgCommons.MinMinerFee * 4) - currentValue
}
