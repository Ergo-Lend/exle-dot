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

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .withId("lendbackend")
  .settings(commonSettings)
  .settings(moduleName := "lendbackend", name := "LendBackend")
  .dependsOn(chain, pay, singleLender)

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

lazy val singleLender = utils
  .mkModule("single-lender", "SingleLender")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Ergo ++
        Testing
  )
  .dependsOn(Seq(common, chain, db).map(_ % allConfigDependency): _*)

lazy val tools = utils
  .mkModule("exle-tools", "ExleTools")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Ergo ++
        Testing
  )
  .dependsOn(Seq(common, singleLender).map(_ % allConfigDependency): _*)


assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x                             => MergeStrategy.first
}

assembly / assemblyJarName := s"${name.value}-${version.value}.jar"
