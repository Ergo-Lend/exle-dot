name := """ergo-dot"""
organization := "dot"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.10"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test

resolvers ++= Seq("Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")

val ergoDevVer = "develop-dd40e4e5-SNAPSHOT"
val ergoLatestVer = "4.0.6"

libraryDependencies ++= Seq(
  jdbc,
  "org.postgresql" % "postgresql" % "42.2.24",
  "org.ergoplatform" %% "ergo-playground-env" % "0.0.0-86-400c8c4b-SNAPSHOT",
  "com.h2database" % "h2" % "1.4.200",
  "org.playframework.anorm" %% "anorm" % "2.6.10",
  "org.ergoplatform" %% "ergo-appkit" % ergoLatestVer,
  "org.scorexfoundation" %% "scrypto" % "2.1.10",
  "com.dripower" %% "play-circe" % "2712.0",
  "com.typesafe.play" %% "play-slick" % "4.0.0",
  "com.typesafe.slick" %% "slick" % "3.3.0",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.0",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
)