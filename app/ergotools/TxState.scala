package ergotools

/**
 * Unsuccessful - Tx is not sent to mempool (0)
 * Mempooled -  Tx is in mempooled but not confirmed (not mined yet) (2)
 * Mined - Successful (1)
 */
object TxState extends Enumeration {
  type TxState = Value
  val Unsuccessful, Mempooled, Mined = Value
}
