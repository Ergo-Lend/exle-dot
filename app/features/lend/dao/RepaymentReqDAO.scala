package features.lend.dao

import ergotools.TxState.TxState

import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

trait RepaymentReqComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  class RepaymentReqTable(tag: Tag) extends Table[RepaymentReq](tag, "REPAYMENT_REQUESTS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def ergAmount = column[Long]("ERG_AMOUNT")
    def repaymentDeadline = column[Long]("REPAYMENT_DEADLINE")

    def state = column[Int]("TX_STATE")
    def paymentAddress = column[String]("PAYMENT_ADDRESS")
    def lendToken = column[String]("LEND_TOKEN")
    def repaymentTxID = column[String]("REPAYMENT_TX_ID")
    def userAddress = column[String]("USER_ADDRESS")

    def timeStamp = column[String]("TIME_STAMP")
    def ttl = column[Long]("TTL")
    def deleted = column[Boolean]("DELETED")

    def * = (id, ergAmount, repaymentDeadline, state, paymentAddress, lendToken, repaymentTxID.?, userAddress,
      timeStamp, ttl, deleted) <> (RepaymentReq.tupled, RepaymentReq.unapply)
  }
}

@Singleton
class RepaymentReqDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) (implicit executionContext: ExecutionContext)
  extends RepaymentReqComponent
    with HasDatabaseConfigProvider[JdbcProfile] with DAO {

  import profile.api._

  val requests = TableQuery[RepaymentReqTable]

  /**
   *
   */
  def insert(ergAmount: Long, repaymentDeadline: Long, state: TxState, paymentAddress: String, lendToken: String, repaymentTxID: Option[String],
             walletAddress: String, timeStamp: String, ttl: Long): Future[Unit] = {
    val action = requests += RepaymentReq(
      id = 1,
      ergAmount = ergAmount,
      repaymentDeadline = repaymentDeadline,
      state = state.id,
      lendToken = lendToken,
      userAddress = walletAddress,
      paymentAddress = paymentAddress,
      repaymentTxID = repaymentTxID,
      timeStamp = timeStamp,
      ttl = ttl,
      deleted = false)
    db.run(action.asTry).map(_ => ())
  }

  def all: Future[Seq[RepaymentReq]] = db.run(requests.filter(_.deleted === false).result)

  def byId(id: Long): Future[RepaymentReq] = db.run(requests.filter(_.deleted === false).filter(req => req.id === id).result.head)

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
    val q = for { c <- requests if c.id === id } yield c.repaymentTxID
    val updateAction = q.update(TxId)
    Await.result(db.run(updateAction), Duration.Inf)
  }

  def updateTTL(id: Long, ttl: Long): Int = {
    val q = for { c <- requests if c.id === id } yield c.ttl
    val updateAction = q.update(ttl)
    Await.result(db.run(updateAction), Duration.Inf)
  }
}
