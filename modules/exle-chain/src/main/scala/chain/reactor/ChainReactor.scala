package chain.reactor

import chain.reactor.LendTxType.{values, LendTxType, SingleLenderErg, Unknown}
import chain.reactor.chambers.SingleLenderErgChamber
import scorex.crypto.authds.merkle.sparse.BlockchainSimulator.Transaction

import scala.collection.mutable.ListBuffer

object LendTxType extends Enumeration {
  type LendTxType = Value
  val SingleLenderErg, SingleLenderToken, Unknown = Value

  def getEnum(string: String): LendTxType =
    values
      .find(_.toString.toLowerCase() == string.toLowerCase())
      .getOrElse(Unknown)
}

/**
  * Reactor
  *
  * A reactor is a lab that consists of many chambers
  * The job of the reactor is to facilitate the operation
  * of each chambers to carry out its atomic fusions
  * and then provide the results back to the user.
  *
  * Procedure:
  * 1. Sets up all required Chambers that are being used
  * 2. Takes in AtomicFusions and inserts into Chambers
  * for cultivation
  * 3. Ensures Chambers successfully fuses all AtomicFusions
  * 4. Notify users on its success
  * 5. Clean up Chambers
  */
trait Reactor {

  /**
    * React is equivalent to running the tx
    * @param request
    * @return position of the chamber in the list
    */
  def setChamber(transaction: LendTxType): Int

  def setChamber(transaction: String): Int = {
    val lendTxType = LendTxType.getEnum(transaction)
    if (lendTxType != LendTxType.Unknown)
      setChamber(lendTxType)
    else
      throw new ChamberNotFoundException()
  }

  def runChamber(): Boolean
  def isChamberReactionComplete(transaction: Transaction): Boolean
  def cleanChamber(): Boolean

  def unsetChamber(): Unit =
    chamber = null

  def ensureChamber(): Boolean =
    if (chamber != null) {
      true
    } else {
      throw new ChamberNotFoundException()
    }
  def mainChamber: Chamber = chamber

  def +(chamber: Chamber) = {
//    chambers ++ chamber
  }

  protected var chamber: Chamber = null
  var chambers: ListBuffer[Chamber] = ListBuffer()
  val chambersCatalog: Map[LendTxType, Chamber]
}

/**
  * Chain Reactor
  *
  * The ideal version of the reactor will receive a request and runs it.
  * The only job it has is to run transaction and react with the chain.
  */
class LendReactor extends Reactor {
  import chain.reactor.LendReactor.Chambers

  /**
    * React is equivalent to running the tx
    *
    * @param request
    * @return
    */
  override def setChamber(transaction: LendTxType): Int = {
    chamber = chambersCatalog.get(transaction).orNull
    ensureChamber()
    chambers = chambers :+ (chamber)
    chambers.size
  }

  override def runChamber(): Boolean = {
    ensureChamber()
    chamber.run()
  }

  override def isChamberReactionComplete(transaction: Transaction): Boolean = {
    ensureChamber()
    chamber.isComplete
  }

  override def cleanChamber(): Boolean = {
    ensureChamber()
    chamber.cleanup()
  }

  override val chambersCatalog: Map[LendTxType, Chamber] = Chambers
}

object LendReactor {

  val Chambers: Map[LendTxType, Chamber] = {
    Map(
      SingleLenderErg -> new SingleLenderErgChamber
    )
  }
}
