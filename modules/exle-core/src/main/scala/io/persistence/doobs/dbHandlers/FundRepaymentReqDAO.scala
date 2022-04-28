package io.persistence.doobs.dbHandlers

import ergo.TxState.TxState
import io.persistence.doobs.models.FundRepaymentReq
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

trait RepaymentReqComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  class RepaymentReqTable(tag: Tag) extends Table[FundRepaymentReq](tag, "repayment_requests") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def repaymentBoxId = column[String]("repayment_box_id")
    def ergAmount = column[Long]("erg_amount")

    def state = column[Int]("tx_state")
    def paymentAddress = column[String]("payment_address")
    def repaymentTxID = column[String]("repayment_tx_id")
    def userAddress = column[String]("user_address")

    def timeStamp = column[String]("time_stamp")
    def ttl = column[Long]("ttl")
    def deleted = column[Boolean]("deleted")

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
