import dependencies._
import utils.commonScalacOptions

name := """exle-dot"""
organization := "io.exle"

version := "1.0"
scalaVersion := "2.12.15"

lazy val NexusReleases = "Sonatype Releases".at(
  "https://s01.oss.sonatype.org/content/repositories/releases"
)

lazy val NexusSnapshots = "Sonatype Snapshots".at(
  "https://s01.oss.sonatype.org/content/repositories/snapshots"
)

lazy val commonSettings = List(
  scalacOptions ++= commonScalacOptions,
  scalaVersion := "2.12.15",
  organization := "io.exle",
  version := "0.1",
  coverageEnabled := false,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("public"),
    Resolver.sonatypeRepo("snapshots"),
    NexusReleases,
    NexusSnapshots
  ),
  libraryDependencies ++= Testing ++
    Enumeratum
)

lazy val allConfigDependency = "compile->compile;test->test"

// ===================== Modules ===================== //
lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .withId("lendbackend")
  .settings(commonSettings)
  .settings(moduleName := "lendbackend", name := "LendBackend")
  .dependsOn(chain, pay, singleLender)

// =================== Base Modules ====================== //
// Description  : Base Modules are modules that is at the bottom
//            of the module hierarchy, and normally other modules
//            depends on it more, and have minimal dependencies
//            other than test modules.
//            Think Raw Materials, like Metal, Wood, Stone

// #NOTE Don't add more stuff into commons unless it makes sense
// We would like to shed out the unnecessary stuffs.
//
// What should commons have?
//  Things that are exle specific, but not ergo specific.
lazy val common = utils
  .mkModule("exle-common", "ExleCommon")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Ergo ++
        Testing ++
        HttpDep ++
        DependencyInjection
  )

lazy val generics = utils
  .mkModule("exle-generics", "ExleGenerics")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Ergo ++
        Testing
  )
  .dependsOn(Seq(testCommons).map(_ % allConfigDependency): _*)

// ======================== Tools & Utilities Modules ================== //
// Description    : Modules that has certain utilities in it, are used by
//              other modules. However, are not at the very bottom of the
//              hierarchy.
//              Think Tools, like Axe, Shovels
lazy val chain = utils
  .mkModule("exle-chain", "ExleChain")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Ergo ++
        Testing ++
        DependencyInjection
  )
  .dependsOn(common)

lazy val pay = utils
  .mkModule("exle-pay", "ExlePay")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Ergo ++
        Testing ++
        Circe ++
        DependencyInjection
  )
  .dependsOn(common)

lazy val db = utils
  .mkModule("db", "Doobs")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      PostgresDB ++
        Testing ++
        PlayApi ++
//        DoobieDB ++
        DependencyInjection
  )
  .dependsOn(common)

lazy val tools = utils
  .mkModule("exle-tools", "ExleTools")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Ergo ++
        Testing
  )
  .dependsOn(Seq(common, singleLender).map(_ % allConfigDependency): _*)

// ====================== Feature Modules ===================== //
// Description    : Modules that carries out certain features, and does
//                that job.
//                Think a Business or System, like a Cashier system in a shop
lazy val singleLender = utils
  .mkModule("single-lender", "SingleLender")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Ergo ++
        Testing ++
        Cats
  )
  .dependsOn(
    Seq(generics, common, chain, db, testCommons)
      .map(_ % allConfigDependency): _*
  )

lazy val exleBot = utils
  .mkModule("exle-bot", "ExleBot")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Ergo ++
        Testing
  )
  .dependsOn(
    Seq(common, db, singleLender).map(_ % allConfigDependency): _*
  )

// =============== Test Modules ============== //
// Description  : Base modules for testing
lazy val testCommons = utils
  .mkModule("exle-test-commons", "ExleTestCommons")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Ergo ++
        Testing
  )
  .dependsOn(common)

// ==== Modules END ==== //

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x                             => MergeStrategy.first
}

assembly / assemblyJarName := s"${name.value}-${version.value}.jar"
