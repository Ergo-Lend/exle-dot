package lendcore.io.persistence.doobs.dbHandlers

import lendcore.components.ergo.TxState.TxState
import lendcore.io.persistence.doobs.models.FundLendReq
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

trait FundLendReqComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  class FundLendReqTable(tag: Tag) extends Table[FundLendReq](tag, "fund_lend_requests") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def lendBoxId = column[String]("lend_box_id")
    def ergAmount = column[Long]("erg_amount")

    def state = column[Int]("tx_state")
    def paymentAddress = column[String]("payment_address")
    def lendToken = column[String]("lend_token")
    def lendTxID = column[String]("lend_tx_id")
    def lenderAddress = column[String]("lender_address")

    def timeStamp = column[String]("time_stamp")
    def ttl = column[Long]("ttl")
    def deleted = column[Boolean]("deleted")

    def * =
      ( id,
        lendBoxId,
        ergAmount,
        state,
        paymentAddress,
        lendTxID.?,
        lenderAddress,
        timeStamp,
        ttl,
        deleted) <> (FundLendReq.tupled, FundLendReq.unapply)
  }
}

@Singleton
class FundLendReqDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) (implicit executionContext: ExecutionContext)
  extends FundLendReqComponent
    with HasDatabaseConfigProvider[JdbcProfile] with DAO {

  import profile.api._

  val requests = TableQuery[FundLendReqTable]

  /**
   *
   */
  def insert(lendBoxId: String,
             fundingErgAmount: Long,
             state: TxState,
             paymentAddress: String,
             lendTxID: Option[String],
             walletAddress: String,
             timeStamp: String,
             ttl: Long): Future[Unit] = {
    val action = requests += FundLendReq(
      id = 1,
      lendBoxId = lendBoxId,
      ergAmount = fundingErgAmount,
      state = state.id,
      lenderAddress = walletAddress,
      paymentAddress = paymentAddress,
      lendTxID = lendTxID,
      timeStamp = timeStamp,
      ttl = ttl,
      deleted = false)
    db.run(action.asTry).map(_ => ())
  }

  def all: Future[Seq[FundLendReq]] = db.run(requests.filter(_.deleted === false).result)

  def byId(id: Long): Future[FundLendReq] = db.run(requests.filter(_.deleted === false).filter(req => req.id === id).result.head)

  def deleteById(id: Long): Future[Int] = db.run(
    requests.filter(req => req.id === id).map(req => req.deleted).update(true)
  )

  def deleteTxById(id: Long): Future[Int] = db.run(
    requests.filter(req => req.id === id).delete
  )

  def updateStateById(id: Long, state: TxState): Future[Int] = {
    val q = for { c <- requests if c.id === id } yield c.state
    val updateAction = q.update(state.id)
    db.run(updateAction)
  }

  def updateLendTxId(id: Long, TxId: String): Int = {
    val q = for { c <- requests if c.id === id } yield c.lendTxID
    val updateAction = q.update(TxId)
    Await.result(db.run(updateAction), Duration.Inf)
  }

  def updateTTL(id: Long, ttl: Long): Int = {
    val q = for { c <- requests if c.id === id } yield c.ttl
    val updateAction = q.update(ttl)
    Await.result(db.run(updateAction), Duration.Inf)
  }
}
