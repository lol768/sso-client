
def coreVersion = "2.7-SNAPSHOT"

lazy val play = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"
//compileOrder := CompileOrder.JavaThenScala

organization := "uk.ac.warwick.sso"
name := """sso-client-play"""
version := coreVersion

// Check local Maven, so we can install ssoclient locally and depend on it during build.
resolvers += "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
resolvers += DefaultMavenRepository
resolvers += "oauth" at "http://oauth.googlecode.com/svn/code/maven"

val appDeps = Seq(
  "uk.ac.warwick.sso" % "sso-client-core" % coreVersion,
  jdbc,
  "xerces" % "xercesImpl" % "2.11.0",
  "xalan" % "xalan" % "2.7.1"
)
libraryDependencies ++= appDeps

dependencyOverrides += "xml-apis" % "xml-apis" % "1.4.01"

val testDeps = Seq(
  "org.scalatest" %% "scalatest" % "2.2.1",
  "org.scalatestplus" %% "play" % "1.4.0-M3",
  "org.scalacheck" %% "scalacheck" % "1.12.5",
  "org.mockito" % "mockito-all" % "1.10.19"
).map(_ % Test)
libraryDependencies ++= testDeps

