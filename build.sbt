organization := "nl.amc.ebioscience"

name := """Rosemary-Vanilla"""

version := "1.2.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature","-language:reflectiveCalls")

libraryDependencies ++= Seq(
  cache,
//  ws,
  "org.keyczar" % "keyczar" % "0.71h",
  "com.github.shayanlinux" %% "play-plugins-salat" % "1.6.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.1",
  "org.apache.lucene" % "lucene-core" % "4.9.1",
  "org.apache.lucene" % "lucene-queryparser" % "4.9.1",
  "org.apache.lucene" % "lucene-queries" % "4.9.1",
  "org.apache.lucene" % "lucene-analyzers-common" % "4.9.1",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "com.github.lookfirst" % "sardine" % "5.6",
  "nl.amc.ebioscience" %% "processing-manager-core" % "2.0-SNAPSHOT",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// ebioscience artifactory resolver because org.keyczar.keyczar is not published in maven central yet
resolvers += "ebioscience-artifactory" at "http://dev.ebioscience.amc.nl/artifactory/public"

routesImport += "se.radley.plugin.salat.Binders._"

TwirlKeys.templateImports += "org.bson.types.ObjectId"

// eclipse plugin configurations
EclipseKeys.withSource := true

EclipseKeys.withJavadoc := true

// setting a maintainer which is used for all packaging types
NativePackagerKeys.maintainer := "shayanlinux"

// exposing the play ports
NativePackagerKeys.dockerExposedPorts in Docker := Seq(9000, 9443)

// create image with: sbt docker:publishLocal
// run container with: docker run -p 9000:9000 <name>:<version>
