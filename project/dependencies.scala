import play.sbt.PlayImport.{guice, jdbc}
import sbt.Keys.libraryDependencies
import sbt._
import versions._

object dependencies {

  val Ergo: List[ModuleID] = List(
    "org.scorexfoundation" %% "scrypto"     % ScryptoVersion,
    "org.ergoplatform"     %% "ergo-appkit" % ErgoAppKitVersion,
    "org.scorexfoundation" %% "sigma-state" % SigmaStateVersion
  )

  val Circe: List[ModuleID] = List(
    "com.dripower" %% "play-circe" % PlayCirceVersion
  )

  val PostgresDB: List[ModuleID] = List(
    jdbc,
    "org.postgresql"     % "postgresql"      % PostgresqlVersion,
    "com.typesafe.slick" %% "slick"          % SlickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion
  )

  val Cats: List[ModuleID] = List(
    "org.typelevel" %% "cats-core" % CatsVersion
  )

  val DoobieDB: List[ModuleID] = List(
    "org.tpolecat" %% "doobie-core"     % DoobieVersion,
    "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
    "org.tpolecat" %% "doobie-hikari"   % DoobieVersion,
    "io.estatico"  %% "newtype"         % NewTypeVersion
  )

  val PlayApi: List[ModuleID] = List(
    "org.playframework.anorm" %% "anorm"      % AnormVersion,
    "com.typesafe.play"       %% "play-slick" % PlaySlickVersion
  )

  val HttpDep: List[ModuleID] = List(
    "org.scalaj" %% "scalaj-http" % ScalaJHttpVersion
  )

  val Testing: List[ModuleID] = List(
    "org.scalatestplus.play" %% "scalatestplus-play" % ScalaTestPlusPlayVersion % Test
  )

  // Java
  val DependencyInjection: List[ModuleID] = List(
    guice
  )

  // Commons
  val Enumeratum: List[ModuleID] = List(
    "com.beachape" %% "enumeratum" % EnumeratumVersion
  )
}
