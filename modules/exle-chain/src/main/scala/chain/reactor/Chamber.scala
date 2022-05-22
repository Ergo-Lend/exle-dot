package chain.reactor

import chain.reactor.ChamberStatus.ChamberStatus
import chain.reactor.LendTxType.LendTxType

object ChamberStatus extends Enumeration {
  type ChamberStatus = Value
  val Started, Reacting, Done, CleanedUp, Default = Value

  def getEnum(string: String): ChamberStatus =
    values
      .find(_.toString.toLowerCase() == string.toLowerCase())
      .getOrElse(Default)
}

/**
  * Chamber
  *
  * A chamber is a piece within the chain.reactor,
  * after determining the type of tx request,
  * the chain.reactor will activate the right chamber
  * and run the requests.
  *
  * A chamber contains many AtomicFusions(Txs) that
  * can be chained together if needed.
  *
  *
  */
abstract class Chamber(val lendTxType: LendTxType) {
  def setup(): Boolean
  def run(): Boolean
  def cleanup(): Boolean
  def isComplete: Boolean = (status == ChamberStatus.Done)
  var status: ChamberStatus
  var reactedTx: String
}
