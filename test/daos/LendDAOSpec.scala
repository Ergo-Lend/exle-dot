//package daos
//
//import ergotools.TxState
//import features.lend.dao.{CreateLendReq, CreateLendReqDAO}
//import helpers.Time
//import org.scalatest.Ignore
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatestplus.play.guice.GuiceOneAppPerSuite
//import org.scalatestplus.play.PlaySpec
//import play.api.Application
//import play.api.inject.guice.GuiceApplicationBuilder
//
//@Ignore
//class LendDAOSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures {
//  implicit override lazy val app = new GuiceApplicationBuilder().
//    configure(
////      "slick.dbs.mydb.driver" -> "slick.driver.PostgresDriver$",
////      "slick.dbs.mydb.db.driver" -> "org.postgresql.Driver",
////      "slick.dbs.mydb.db.url" -> "jdbc:postgresql://localhost:5432/test",
////      "slick.dbs.mydb.db.user" -> "",
////      "slick.dbs.mydb.db.password" -> ""
//        "slick.dbs.mydb.driver" -> "slick.driver.H2Profile$",
//      "slick.dbs.default.driver" -> "slick.driver.H2Driver$",
//        "slick.dbs.mydb.db.driver" -> "org.h2.Driver",
//        "slick.dbs.mydb.db.url" -> "jdbc:h2:tcp://localhost/~/test",
//        "slick.dbs.mydb.db.user" -> "sa",
//        "slick.dbs.mydb.db.password" -> ""
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
