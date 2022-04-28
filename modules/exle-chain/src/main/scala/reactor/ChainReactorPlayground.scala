package reactor

object ChainReactorPlayground {
  def main(args: Array[String]): Unit = {
    val lendReactor = new LendReactor()
    lendReactor setChamber LendTxType.SingleLenderErg
    lendReactor runChamber
    val chamber = lendReactor.mainChamber
  }
}
