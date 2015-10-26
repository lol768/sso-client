
def libraryVersion = "2.7"

lazy val play = (project in file(".")).enablePlugins(PlayScala)

// Can add 2.12 to this as and when we need to support that
// then you prepend + to a task to cross-run it, e.g. sbt +publish
crossScalaVersions := Seq("2.11.6")

scalaVersion := "2.11.6"
publishMavenStyle := true
compileOrder := CompileOrder.ScalaThenJava // maybe faster?

organization := "uk.ac.warwick.sso"
name := """sso-client-play"""
version := libraryVersion

// Check local Maven, so we can install ssoclient locally and depend on it during build.
resolvers += "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
resolvers += WarwickNexus
resolvers += DefaultMavenRepository
resolvers += "oauth" at "http://oauth.googlecode.com/svn/code/maven"

repositorySettings

val appDeps = Seq[ModuleID](
  "uk.ac.warwick.sso" % "sso-client-core" % libraryVersion,
  jdbc,
  "xerces" % "xercesImpl" % "2.11.0",
  "xalan" % "xalan" % "2.7.1"
)
libraryDependencies ++= appDeps

//dependencyOverrides += "xml-apis" % "xml-apis" % "1.4.01"

val testDeps = Seq[ModuleID](
  "org.scalatest" %% "scalatest" % "2.2.1",
  "org.scalatestplus" %% "play" % "1.4.0-M3",
  "org.scalacheck" %% "scalacheck" % "1.12.5",
  "org.mockito" % "mockito-all" % "1.10.19"
).map(_ % Test)
libraryDependencies ++= testDeps

