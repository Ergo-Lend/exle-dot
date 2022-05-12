package reactor

import reactor.FusionStatus.FusionStatus
import org.ergoplatform.appkit.{
  BlockchainContext,
  InputBox,
  UnsignedTransactionBuilder
}

object FusionStatus extends Enumeration {
  type FusionStatus = Value
  val Unsuccessful, Mempooled, Mined, Completed, ScriptFalsed = Value
}

/**
  * AtomicFusion
  *
  * A fusion is when a transaction is modeled and
  * sent to the chain.
  *
  * Procedure:
  * 1. Instantiated with necessary Inputs for tx
  * to happen: InputBox, txB, ctx, chainedID, outputBox
  * 2. Signed and sent
  * 3. Get ChainedId for linking to next fusion
  * 4. Check for confirmation (for notification purposes too)
  *
  */
abstract class AtomicFusion {

  /**
    * Equivalent to Refunding or canceling a transaction
    * @return
    */
  def defuse(): Boolean

  /**
    * !AtomicFusion will defuse the fusion
    * @return
    */
  def unary_! : Boolean =
    defuse()

  /**
    * Carry out the transaction and send it to the blockchain
    * @return
    */
  def fuse(): Boolean

  /**
    * AtomicFusion() will call fuse.
    * AtomicFusion.apply() == AtomicFusion()
    * @return
    */
  def apply(): Boolean =
    fuse()

  /**
    * Check to see if the tx was successful
    * @return
    */
  def isFusionSuccessful(): Boolean

  /**
    * Gets the status of a fusion
    * @return
    */
  def status: FusionStatus

  /**
    * Receives a ChainID as an input for a chained Tx to happen
    * @param id
    */
  def withChainedId(id: String): Boolean

  /**
    * Transaction Builder that will be used for the transaction
    * @param txB
    * @return
    */
  def withTxBuilder(inputTxB: UnsignedTransactionBuilder): Unit =
    // We want to ensure we can change the txB if needed to
    txB = inputTxB

  def withCtx(inputCtx: BlockchainContext): Unit =
    if (ctx == null)
      ctx = inputCtx

  /**
    * Inputs for the tx
    * @param inputs
    */
  def withInputs(inputs: List[InputBox]): Unit

  /**
    * Get the chained Id for the
    * @return
    */
  def getChainedId(): String

  var ctx: BlockchainContext = _
  var txId: String = _
  var txB: UnsignedTransactionBuilder = _
}
