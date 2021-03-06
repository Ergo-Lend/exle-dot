package chain.explorer

import org.ergoplatform.appkit.{ErgoId, NetworkType}

object ExplorerPlayground {

  def main(args: Array[String]) = {
    val explorerHandler = new ExplorerHandler(NetworkType.MAINNET)
    val myTx = explorerHandler.getTransaction(
      Helpers.toId(
        "28741208770452033e3ccb09e27fcbe3aa5734ce9329d46938fee6f630587242"
      )
    )
    val lendBoxes = explorerHandler.getBoxesByTokenId(ErgoId.create(""))

    if (lendBoxes.isDefined) {
      val boxes = lendBoxes.get
      println(boxes.length)
      boxes.toList.map(output => println(output))
    }

    println("done")
  }
}
