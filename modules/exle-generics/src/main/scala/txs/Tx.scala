package txs

import commons.configs.ServiceConfig
import commons.ergo.ErgCommons
import commons.errors.{ProveException, ReducedException}
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.{BlockchainContext, InputBox, OutBox, ReducedTransaction, SignedTransaction, UnsignedTransaction, UnsignedTransactionBuilder}

import scala.collection.JavaConverters.seqAsJavaListConverter

trait Tx {
  var signedTx: Option[SignedTransaction] = None
  val inputBoxes: Seq[InputBox]
  val dataInputs: Seq[InputBox] = Seq.empty
  val changeAddress: P2PKAddress = ServiceConfig.serviceOwner.asP2PK()
  implicit val ctx: BlockchainContext

  def getOutBoxes: Seq[OutBox]

  def getOutBoxesAsInputBoxes(txId: String): Seq[InputBox] =
    // Increment number
    getOutBoxes.zipWithIndex.map {
      case (box, count) => box.convertToInputWith(txId, count.toShort)
    }

  def buildTx: UnsignedTransaction = {
    val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
    val outBoxes: Seq[OutBox] = getOutBoxes

    val tx: UnsignedTransaction = dataInputs match {
      case Nil =>
        txB
          .boxesToSpend(inputBoxes.asJava)
          .outputs(outBoxes: _*)
          .fee(ErgCommons.MinMinerFee)
          .sendChangeTo(changeAddress)
          .build()
      case _ =>
        txB
          .boxesToSpend(inputBoxes.asJava)
          .outputs(outBoxes: _*)
          .withDataInputs(dataInputs.asJava)
          .fee(ErgCommons.MinMinerFee)
          .sendChangeTo(changeAddress)
          .build()
    }
    tx
  }

  def signTx: SignedTransaction =
    try {
      signedTx = Option(ctx.newProverBuilder().build().sign(buildTx))

      signedTx.get
    } catch {
      case e: Throwable => {
        throw ProveException(e.getMessage)
      }
    }

  def reduceTx: ReducedTransaction =
    try {
      ctx.newProverBuilder().build().reduce(buildTx, 0)
    } catch {
      case e: Throwable =>
        throw ReducedException(e.getMessage)
    }
}

/**
  * Tx Route is the route that an input box/multiple input boxes takes
  * when undergoing a transaction.
  *
  * ** One to Many **
  * For instance, a lendbox when undergoing a "mutation" to repayment box
  * will transformed into 2 boxes:
  * 1. Borrower Fund Box
  * 2. Repayment Box
  *
  * ** Many to One **
  * And on the other hand, when it goes through a "funding" route, it will
  * transformed into 1 box:
  * 1. LendBox with new value.
  * But, its input is actually 2 boxes:
  * 1. LendBox
  * 2. PaymentBox
  *
  * ** One to One **
  * A service box, goes through multiple cases where the only resulting box
  * is itself.
  *
  * ** Many to Many **
  * Lastly, the many to many case. Within the Single Lender protocol, there
  * are no many to many cases that is significant.
  *
  * In short,
  * TxRoutes are routes that are specific for individual cases/boxes,
  * and it is use to build up to a complete Txs
  *
  * NOTE: TxRoute is a supplemental class to boxes.
  * There is a primary box within a tx route. For example a LendBox
  * LendBox takes a payment box to form a OutLendBox
  *
  * or a ServiceBox
  */
trait TxRoute {
  val inputBoxes: Seq[InputBox]
  implicit val ctx: BlockchainContext

  def getOutBoxes: Seq[OutBox]
}
