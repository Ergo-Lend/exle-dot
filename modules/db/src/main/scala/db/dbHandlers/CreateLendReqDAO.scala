package db.dbHandlers

import commons.TxState.TxState
import db.models.CreateLendReq
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

trait CreateLendReqComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  class CreateLendReqTable(tag: Tag)
      extends Table[CreateLendReq](tag, "create_requests") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[String]("description")
    def goal = column[Long]("goal")
    def creationHeight = column[Long]("creation_height")
    def deadlineHeight = column[Long]("deadline_height")
    def repaymentHeight = column[Long]("repayment_height")
    def interestRate = column[Long]("interest_rate")

    def state = column[Int]("tx_state")
    def borrowerAddress = column[String]("borrower_address")
    def paymentAddress = column[String]("payment_address")
    def createTxId = column[String]("create_tx_id")

    def timeStamp = column[String]("time_stamp")
    def ttl = column[Long]("ttl")
    def deleted = column[Boolean]("deleted")

    def * =
      (
        id,
        name,
        description,
        goal,
        creationHeight,
        deadlineHeight,
        repaymentHeight,
        interestRate,
        state,
        borrowerAddress,
        paymentAddress,
        createTxId.?,
        timeStamp,
        ttl,
        deleted
      ) <> (CreateLendReq.tupled, CreateLendReq.unapply)
  }
}

@Singleton
class CreateLendReqDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit executionContext: ExecutionContext)
    extends CreateLendReqComponent
    with HasDatabaseConfigProvider[JdbcProfile]
    with DAO {

  import profile.api._

  val requests = TableQuery[CreateLendReqTable]

  /**
    *
    */
  def insert(
    name: String,
    description: String,
    goal: Long,
    creationHeight: Long,
    deadlineHeight: Long,
    repaymentHeight: Long,
    interestRate: Long,
    state: TxState,
    walletAddress: String,
    paymentAddress: String,
    createTxId: Option[String],
    timeStamp: String,
    ttl: Long
  ): Future[Unit] = {
    val action = requests += CreateLendReq(
      id = 1,
      name = name,
      description = description,
      goal = goal,
      creationHeight = creationHeight,
      deadlineHeight = deadlineHeight,
      repaymentHeight = repaymentHeight,
      interestRate = interestRate,
      state = state.id,
      borrowerAddress = walletAddress,
      paymentAddress = paymentAddress,
      createTxId = createTxId,
      timeStamp = timeStamp,
      ttl = ttl,
      deleted = false
    )
    db.run(action.asTry).map(_ => ())
  }

  def all: Future[Seq[CreateLendReq]] =
    db.run(requests.filter(_.deleted === false).result)

  def byId(id: Long): Future[CreateLendReq] =
    db.run(
      requests
        .filter(_.deleted === false)
        .filter(req => req.id === id)
        .result
        .head
    )

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

  def updateCreateTxID(id: Long, TxId: String): Int = {
    val q = for { c <- requests if c.id === id } yield c.createTxId
    val updateAction = q.update(TxId)
    Await.result(db.run(updateAction), Duration.Inf)
  }

  def updateTTL(id: Long, ttl: Long): Int = {
    val q = for { c <- requests if c.id === id } yield c.ttl
    val updateAction = q.update(ttl)
    Await.result(db.run(updateAction), Duration.Inf)
  }
}

trait DAO {
  def updateTTL(id: Long, ttl: Long): Int
  def updateStateById(id: Long, state: TxState): Future[Int]
}
