package features.lend.dao

import ergotools.TxState.TxState

import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

trait FundLendReqComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  class FundLendReqTable(tag: Tag) extends Table[FundLendReq](tag, "FUND_LEND_REQUESTS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def ergAmount = column[Long]("ERG_AMOUNT")
    def lendDeadline = column[Long]("LEND_DEADLINE")

    def state = column[Int]("TX_STATE")
    def paymentAddress = column[String]("PAYMENT_ADDRESS")
    def lendToken = column[String]("LEND_TOKEN")
    def lendTxID = column[String]("LEND_TX_ID")
    def lenderAddress = column[String]("LENDER_ADDRESS")

    def timeStamp = column[String]("TIME_STAMP")
    def ttl = column[Long]("TTL")
    def deleted = column[Boolean]("DELETED")

    def * = (id, ergAmount, lendDeadline, state, paymentAddress, lendToken, lendTxID.?, lenderAddress,
      timeStamp, ttl, deleted) <> (FundLendReq.tupled, FundLendReq.unapply)
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
  def insert(ergAmount: Long, lendDeadline: Long, state: TxState, paymentAddress: String, lendToken: String, lendTxID: Option[String],
             walletAddress: String, timeStamp: String, ttl: Long): Future[Unit] = {
    val action = requests += FundLendReq(
      id = 1,
      ergAmount = ergAmount,
      lendDeadline = lendDeadline,
      state = state.id,
      lendToken = lendToken,
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
