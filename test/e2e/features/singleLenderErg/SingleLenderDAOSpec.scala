//package e2e.features.singleLenderErg
//
//import lendcore.ergo.TxState
//import features.lend.dao.{CreateLendReqDAO}
//import lendcore.common.Time
//import org.scalatest.Ignore
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatestplus.play.guice.GuiceOneAppPerSuite
//import org.scalatestplus.play.PlaySpec
//import play.api.Application
//import play.api.inject.guice.GuiceApplicationBuilder
//
//@Ignore
//class LendDAOSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures {
//  implicit override lazy val app = new GuiceApplicationBuilder().
//    configure(
//      "slick.dbs.mydb.driver" -> "slick.driver.PostgresDriver$",
//      "slick.dbs.mydb.db.driver" -> "org.postgresql.Driver",
//      "slick.dbs.mydb.db.url" -> sys.env.get{"JDBC_TEST_URL"},
//      "slick.dbs.mydb.db.user" -> ${"DATABASE_USERNAME"},
//      "slick.dbs.mydb.db.password" -> ${"DATABASE_PASSWORD"}
//    ).build
//
//  def lendDAO(implicit app: Application): CreateLendReqDAO = Application.instanceCache[CreateLendReqDAO].apply(app)
//
//  "LendDAO" should {
//    "do whatever" in {
//      whenReady(lendDAO.all) {res =>
//        println("we're here")
//        println(res)
//      }
//    }
//
//    "get something" in {
//      whenReady(lendDAO.byId(1)) {res =>
//        println(res)
//      }
//    }
//
//    "set to delete" in {
//      whenReady(lendDAO.deleteById(1)) {
//        res =>
//          println(res)
//      }
//    }
//
//    "deleteTx" in {
//      whenReady(lendDAO.deleteTxById(1)) {
//        res =>
//          println(res)
//      }
//    }
//
//    "insert" in {
//      whenReady(lendDAO.insert(
//        "Surviving the Pandemic",
//        "People need some gold gold to survive them pandemic",
//        100,
//        100,
//        10000,
//        10000,
//        10,
//        TxState.Mined,
//        "102ifkx81ms7g12jc8sm1md8j1293md8dj2m1",
//        "10d89k4kf8gkjj48h9d01f0bmnchshdc7dk",
//        null,
//        Time.currentTime.toString,
//        2)) {
//        res =>
//          println(res)
//      }
//    }
//  }
//}
