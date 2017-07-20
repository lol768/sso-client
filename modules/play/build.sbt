
def libraryVersion = "2.31"

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
  scalaVersion := "2.12.2",
  crossScalaVersions := Seq("2.11.8", "2.12.2"),

  publishMavenStyle := true,
  compileOrder := CompileOrder.ScalaThenJava, // maybe faster?

  organization := "uk.ac.warwick.sso",
  version := libraryVersion,

  // Check local Maven, so we can install ssoclient locally and depend on it during build.
  // (this prepends to any existing list, to make sure we actually find up to date snapshots)
  // (https://github.com/sbt/sbt/issues/321)
  resolvers := ("Local Maven Repository" at "file:///" + Path.userHome.absolutePath + "/.m2/repository") +: resolvers.value,
  //resolvers += "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
  resolvers += WarwickNexus,
  resolvers += DefaultMavenRepository,
  resolvers += "oauth" at "http://oauth.googlecode.com/svn/code/maven"
)

lazy val appDeps = Seq[ModuleID](
  guice,
  "uk.ac.warwick.sso" % "sso-client-core" % libraryVersion,
  component("play-jdbc-api"),
  "xerces" % "xercesImpl" % "2.11.0",
  "xalan" % "xalan" % "2.7.1"
)

lazy val testDeps = Seq[ModuleID](
  jdbc,
  component("play-iteratees"),
  component("play-iteratees-reactive-streams"),
  "org.scalatest" %% "scalatest" % "3.0.3",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0",
  "org.scalacheck" %% "scalacheck" % "1.13.5",
  "org.mockito" % "mockito-all" % "1.10.19",
  "com.h2database" % "h2" % "1.4.193"
).map(_ % Test)

// https://bugs.elab.warwick.ac.uk/browse/SSO-1653
dependencyOverrides += "xml-apis" % "xml-apis" % "1.4.01"

