package reactor.chambers

import reactor.ChamberStatus.ChamberStatus
import reactor.{Chamber, LendTxType}

class SingleLenderErgChamber extends Chamber(LendTxType.SingleLenderErg) {
  override def setup(): Boolean = ???

  override def run(): Boolean = ???

  override def cleanup(): Boolean = ???

  override var status: ChamberStatus = _
  override var reactedTx: String = _
}
