
def libraryVersion = "2.10"

lazy val root = (project in file("."))
  .aggregate(library, testing)
  .settings(commonSettings :_*)
  .settings(
    publish := {},
    publishArtifact := false
  )

lazy val library = (project in file("library")).enablePlugins(PlayScala)
  .settings(commonSettings :_*)
  .settings(
    name := """sso-client-play""",
    libraryDependencies ++= appDeps ++ testDeps
  )
  .settings(repositorySettings :_*)

// Helper library for other apps' tests.
lazy val testing = (project in file("testing")).enablePlugins(PlayScala)
  .dependsOn(library)
  .settings(commonSettings :_*)
  .settings(
    name := """sso-client-play-testing""",
    libraryDependencies ++= appDeps ++ testDeps
  )
  .settings(repositorySettings :_*)

lazy val commonSettings = Seq(
  scalaVersion := "2.11.6",
  crossScalaVersions := Seq("2.11.6"),

  publishMavenStyle := true,
  compileOrder := CompileOrder.ScalaThenJava, // maybe faster?

  organization := "uk.ac.warwick.sso",
  version := libraryVersion,

  // Check local Maven, so we can install ssoclient locally and depend on it during build.
  resolvers += "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
  resolvers += WarwickNexus,
  resolvers += DefaultMavenRepository,
  resolvers += "oauth" at "http://oauth.googlecode.com/svn/code/maven"
)

lazy val appDeps = Seq[ModuleID](
  "uk.ac.warwick.sso" % "sso-client-core" % libraryVersion,
  jdbc,
  "xerces" % "xercesImpl" % "2.11.0",
  "xalan" % "xalan" % "2.7.1"
)

lazy val testDeps = Seq[ModuleID](
  "org.scalatest" %% "scalatest" % "2.2.1",
  "org.scalatestplus" %% "play" % "1.4.0-M3",
  "org.scalacheck" %% "scalacheck" % "1.12.5",
  "org.mockito" % "mockito-all" % "1.10.19"
).map(_ % Test)

