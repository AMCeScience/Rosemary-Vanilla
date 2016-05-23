organization := "nl.amc.ebioscience"

name := """Rosemary-Vanilla"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
//  jdbc,
//  cache,
//  ws,
  "org.keyczar" % "keyczar" % "0.71h",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// ebioscience artifactory resolver because org.keyczar.keyczar is not published in maven central yet
resolvers += "ebioscience-artifactory" at "http://dev.ebioscience.amc.nl/artifactory/public"

// eclipse plugin configurations
EclipseKeys.withSource := true

EclipseKeys.withJavadoc := true

// setting a maintainer which is used for all packaging types
NativePackagerKeys.maintainer := "shayanlinux"

// exposing the play ports
NativePackagerKeys.dockerExposedPorts in Docker := Seq(9000, 9443)

// create image with: sbt docker:publishLocal
// run container with: docker run -p 9000:9000 <name>:<version>
