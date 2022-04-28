import dependencies.{DependencyInjection, _}

name := """exle-dot"""
organization := "io.exle"

version := "1.0"
scalaVersion := "2.12.15"

lazy val NexusReleases  = "Sonatype Releases" at "https://s01.oss.sonatype.org/content/repositories/releases"
lazy val NexusSnapshots = "Sonatype Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots"

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
  libraryDependencies ++= dependencies.Testing
)

lazy val allConfigDependency = "compile->compile;test->test"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .withId("lendbackend")
  .settings(commonSettings)
  .settings(moduleName := "lendbackend", name:= "LendBackend")
  .dependsOn(core, chain)

lazy val core = utils
  .mkModule("exle-core", "ExleCore")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Ergo ++
      Circe ++
      PostgresDB ++
      PlayApi ++
      HttpDep ++
      Testing ++
      DependencyInjection
  )
  .dependsOn(Seq(chain, common).map(_ % allConfigDependency): _*)

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

lazy val commonScalacOptions = List(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-feature",
  "-unchecked",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ypartial-unification"
)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

assembly / assemblyJarName := s"${name.value}-${version.value}.jar"
