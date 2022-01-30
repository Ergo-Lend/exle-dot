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

  class RepaymentReqTable(tag: Tag) extends Table[FundRepaymentReq](tag, "REPAYMENT_REQUESTS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def repaymentBoxId = column[String]("REPAYMENT_BOX_ID")
    def ergAmount = column[Long]("ERG_AMOUNT")

    def state = column[Int]("TX_STATE")
    def paymentAddress = column[String]("PAYMENT_ADDRESS")
    def repaymentTxID = column[String]("REPAYMENT_TX_ID")
    def userAddress = column[String]("USER_ADDRESS")

    def timeStamp = column[String]("TIME_STAMP")
    def ttl = column[Long]("TTL")
    def deleted = column[Boolean]("DELETED")

    def * =
      ( id,
        repaymentBoxId,
        ergAmount,
        state,
        paymentAddress,
        repaymentTxID.?,
        userAddress,
        timeStamp,
        ttl,
        deleted) <> (FundRepaymentReq.tupled, FundRepaymentReq.unapply)
  }
}

@Singleton
class FundRepaymentReqDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) (implicit executionContext: ExecutionContext)
  extends RepaymentReqComponent
    with HasDatabaseConfigProvider[JdbcProfile] with DAO {

  import profile.api._

  val requests = TableQuery[RepaymentReqTable]

  /**
   *
   */
  def insert(repaymentBoxId: String,
             fundingErgAmount: Long,
             state: TxState,
             paymentAddress: String,
             repaymentTxID: Option[String],
             walletAddress: String,
             timeStamp: String,
             ttl: Long): Future[Unit] = {
    val action = requests += FundRepaymentReq(
      id = 1,
      repaymentBoxId = repaymentBoxId,
      ergAmount = fundingErgAmount,
      state = state.id,
      paymentAddress = paymentAddress,
      repaymentTxID = repaymentTxID,
      userAddress = walletAddress,
      timeStamp = timeStamp,
      ttl = ttl,
      deleted = false)
    db.run(action.asTry).map(_ => ())
  }

  def all: Future[Seq[FundRepaymentReq]] = db.run(requests.filter(_.deleted === false).result)

  def byId(id: Long): Future[FundRepaymentReq] = db.run(requests.filter(_.deleted === false).filter(req => req.id === id).result.head)

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
