
def coreVersion = "2.7-SNAPSHOT"

lazy val play = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"
//compileOrder := CompileOrder.JavaThenScala

name := """sso-client-play"""

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")


// Check local Maven, so we can install ssoclient locally and depend on it during build.
resolvers += "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
resolvers += DefaultMavenRepository
resolvers += "nexus" at "https://mvn.elab.warwick.ac.uk/nexus/content/groups/public"
resolvers += "oauth" at "http://oauth.googlecode.com/svn/code/maven"


val appDeps = Seq(
  "uk.ac.warwick.sso" % "sso-client" % coreVersion,
  jdbc
)
libraryDependencies ++= appDeps

val testDeps = Seq(
  "org.scalatest" %% "scalatest" % "2.2.1",
  "org.scalatestplus" %% "play" % "1.4.0-M3"
).map(_ % Test)
libraryDependencies ++= testDeps


routesGenerator := InjectedRoutesGenerator
