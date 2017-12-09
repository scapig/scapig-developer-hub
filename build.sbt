import _root_.play.sbt.PlayImport._

import sbt.Keys._
import sbt._

name := "scapig-developer-hub"

version := "1.0"

lazy val `scapig_developer_hub` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
resolvers += Resolver.jcenterRepo

scalaVersion := "2.12.2"

libraryDependencies ++= Seq( ws, guice, play.sbt.PlayImport.ehcache )
libraryDependencies += "com.typesafe.play" %% "play-json-joda" % "2.6.0"
libraryDependencies += "org.raml" % "raml-parser-2" % "1.0.3"

libraryDependencies += "com.mohiva" %% "play-silhouette" % "5.0.2"
libraryDependencies += "com.mohiva" %% "play-silhouette-password-bcrypt" % "5.0.2"
libraryDependencies += "com.mohiva" %% "play-silhouette-persistence" % "5.0.2"
libraryDependencies += "com.mohiva" %% "play-silhouette-crypto-jca" % "5.0.2"
libraryDependencies += "com.iheart" %% "ficus" % "1.4.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % "test"
libraryDependencies += "com.github.tomakehurst" % "wiremock-standalone" % "2.8.0" % "test"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % "component"
libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.3.0" % "component"
libraryDependencies += "com.mohiva" %% "play-silhouette-testkit" % "5.0.2" % "test"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

lazy val microservice = (project in file("."))
  .enablePlugins(Seq(play.sbt.PlayScala) : _*)
  .configs(IntTest)
  .settings(inConfig(IntTest)(Defaults.testSettings): _*)
  .settings(
    Keys.fork in IntTest := false,
    unmanagedSourceDirectories in IntTest <<= (baseDirectory in IntTest) (base => Seq(base / "it"))
  )
  .configs(ComponentTest)
  .settings(inConfig(ComponentTest)(Defaults.testSettings): _*)
  .settings(
    Keys.fork in ComponentTest := false,
    unmanagedSourceDirectories in ComponentTest <<= (baseDirectory in ComponentTest) (base => Seq(base / "component"))
  )

lazy val IntTest = config("it") extend Test
lazy val ComponentTest = config("component") extend Test
