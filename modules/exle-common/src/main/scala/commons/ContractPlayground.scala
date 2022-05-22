package commons

import commons.common.TimeUtils.time
import commons.contracts.{ContractTypes, ExleContract, ExleContracts}

object ContractPlayground extends App {

  val contract: ExleContract = ExleContracts.values
    .filter(_.contractType == ContractTypes.BoxGuardScript)
    .head

  println(s"Contract found: ${contract}")

  val contractScript = time(contract.contractScript)
  println(contractScript)
  println(time(contract.contractScript))
  println(time(contract.contractScript))
}
