package features.lend.dao

import ergotools.TxState.TxState

import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

trait CreateLendReqComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  class CreateLendReqTable(tag: Tag) extends Table[CreateLendReq](tag, "create_requests") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def description = column[String]("DESCRIPTION")
    def goal = column[Long]("GOAL")
    def deadlineHeight = column[Long]("DEADLINE_HEIGHT")
    def repaymentHeight = column[Long]("REPAYMENT_HEIGHT")
    def interestRate = column[Long]("INTEREST_RATE")

    def state = column[Int]("TX_STATE")
    def borrowerAddress = column[String]("BORROWER_ADDRESS")
    def paymentAddress = column[String]("PAYMENT_ADDRESS")
    def createTxId = column[String]("CREATE_TX_ID")

    def timeStamp = column[String]("TIME_STAMP")
    def ttl = column[Long]("TTL")
    def deleted = column[Boolean]("DELETED")

    def * = (id, name, description, goal, deadlineHeight, repaymentHeight, interestRate, state, borrowerAddress, paymentAddress,
      createTxId.?, timeStamp, ttl, deleted) <> (CreateLendReq.tupled, CreateLendReq.unapply)
  }
}

@Singleton
class CreateLendReqDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) (implicit executionContext: ExecutionContext)
  extends CreateLendReqComponent
    with HasDatabaseConfigProvider[JdbcProfile] with DAO {

  import profile.api._

  val requests = TableQuery[CreateLendReqTable]

  /**
   *
   */
  def insert(name: String,
             description: String,
             goal: Long,
             deadlineHeight: Long,
             repaymentHeight: Long,
             interestRatePercent: Long,
             state: TxState,
             walletAddress: String,
             paymentAddress: String,
             createTxId: Option[String],
             timeStamp: String,
             ttl: Long): Future[Unit] = {
    val action = requests += CreateLendReq(
      id = 1,
      name = name,
      description = description,
      goal = goal,
      deadlineHeight = deadlineHeight,
      repaymentHeight = repaymentHeight,
      interestRatePercent = interestRatePercent,
      state = state.id,
      borrowerAddress = walletAddress,
      paymentAddress = paymentAddress,
      createTxId = createTxId,
      timeStamp = timeStamp,
      ttl = ttl,
      deleted = false)
    db.run(action.asTry).map(_ => ())
  }

  def all: Future[Seq[CreateLendReq]] = db.run(requests.filter(_.deleted === false).result)

  def byId(id: Long): Future[CreateLendReq] = db.run(requests.filter(_.deleted === false).filter(req => req.id === id).result.head)

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
