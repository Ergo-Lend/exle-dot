name := """ergo-dot"""
organization := "dot"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
//lazy val core = Project(id = "lendcore", base = file("lendcore"))
//lazy val root = Project(id = "lend-service", base = file(".")).enablePlugins(PlayScala).dependsOn(core)

scalaVersion := "2.12.10"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test

resolvers ++= Seq("Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")

val DoobieVersion = "1.0.0-RC1"
val NewTypeVersion = "0.4.4"

val ergoDevVer = "develop-d90135c5-SNAPSHOT"
val ergoLatestVer = "4.0.6"
val ergoDependencies = Seq(
  "org.scorexfoundation" %% "scrypto" % "2.1.10",
  "org.ergoplatform" %% "ergo-playground-env" % "0.0.0-86-400c8c4b-SNAPSHOT",
  "org.ergoplatform" %% "ergo-appkit" % ergoLatestVer,
)

val circeDependencies = Seq(
  "com.dripower" %% "play-circe" % "2712.0")

val dbDependencies = Seq(
  jdbc,
  "org.postgresql" % "postgresql" % "42.2.24",
  "com.h2database" % "h2" % "1.4.200",
)

val doobieDependencies = Seq(
  "org.tpolecat" %% "doobie-core"     % DoobieVersion,
  "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
  "org.tpolecat" %% "doobie-hikari"   % DoobieVersion,
  "io.estatico"  %% "newtype"         % NewTypeVersion
)

val cats = Seq(
  "org.typelevel" %% "cats-core" % "2.3.0"
)

libraryDependencies ++= Seq(
  "org.playframework.anorm" %% "anorm" % "2.6.10",
  "com.typesafe.play" %% "play-slick" % "4.0.0",
  "com.typesafe.slick" %% "slick" % "3.3.0",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.0",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "org.slf4j" % "slf4j-jdk14" % "1.7.36"
)

libraryDependencies ++=
//  doobieDependencies ++
//      cats ++
      dbDependencies ++
      ergoDependencies ++
      circeDependencies

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

assembly / assemblyJarName := s"${name.value}-${version.value}.jar"
