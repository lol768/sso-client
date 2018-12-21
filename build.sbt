def libraryVersion = "2.60.1" // propagates downwards
def warwickUtilsVersion = "20180823"
def jettyVersion = "8.2.0.v20160908"
def springVersion = "4.1.4.RELEASE"

lazy val root = Project(id="sso-client-project", base = file("."))
  .aggregate(clientCore, clientPlay, clientServlet)
  .settings(commonSettings :_*)
  .settings(
    publish := {},
    publishArtifact := false
  )

// ---------- Start Core ----------


lazy val clientCore = Project(id="sso-client-core", base = file("./modules/core/"))
  .settings(commonSettingsJava: _*)
  .settings(
    name := """sso-client-core""",
    libraryDependencies ++= clientCoreDeps
  )

lazy val clientCoreDeps = Seq(
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % Provided,
  "javax.inject" % "javax.inject" % "1",
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.10",
  "xfire" % "opensaml" % "1.0.1",
  "org.apache.santuario" % "xmlsec" % "1.4.3",
  "taglibs" % "standard" % "1.1.2" % Optional ,
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "org.apache.httpcomponents" % "httpmime" % "4.4.1",
  "commons-configuration" % "commons-configuration" % "1.1",
  "net.oauth.core" % "oauth" % "20090825",
  "net.oauth.core" % "oauth-provider" % "20090531",
  "net.oauth.core" % "oauth-httpclient4" % "20090913",
  "org.apache.httpcomponents" % "httpasyncclient" % "4.1.2",
  "org.apache.httpcomponents" % "httpcore" % "4.4.6",
  "uk.ac.warwick.util" % "warwickutils-cache" % s"$warwickUtilsVersion",
  "uk.ac.warwick.util" % "warwickutils-core" % s"$warwickUtilsVersion",
  "uk.ac.warwick.util" % "warwickutils-web" % s"$warwickUtilsVersion",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.60",
  "net.sf.ehcache" % "ehcache" % "2.9.0" % Optional ,
  "net.spy" % "spymemcached" % "2.10.6" % Optional ,
  "org.slf4j" % "slf4j-simple" % "1.7.10" % "test",
  "uk.ac.warwick.util" % "warwickutils-cache" % s"$warwickUtilsVersion" % "test",
  "junit" % "junit" % "4.12" % "test",
  "org.jmock" % "jmock-junit4" % "2.5.1" % "test",
  "org.hamcrest" % "hamcrest-library" % "1.3" % "test",
  "org.eclipse.jetty" % "jetty-server" % s"$jettyVersion" % "test",
  "org.eclipse.jetty" % "jetty-http" % s"$jettyVersion" % "test",
  "org.eclipse.jetty" % "jetty-io" % s"$jettyVersion" % "test",
  "org.eclipse.jetty" % "jetty-continuation" % s"$jettyVersion" % "test",
  "org.eclipse.jetty" % "jetty-websocket" % s"$jettyVersion" % "test",
  "org.eclipse.jetty" % "jetty-util" % s"$jettyVersion" % "test",
  "org.eclipse.jetty" % "jetty-webapp" % s"$jettyVersion" % "test",
  "org.springframework" % "spring-test" % s"$springVersion" % "test",
  "jmock" % "jmock-cglib" % "1.2.0" % "test",
  "xalan" % "xalan" % "2.7.0" % "test",
  "xerces" % "xercesImpl" % "2.6.0" % "test",
  "org.jruby" % "jruby-complete" % "1.4.0" % "test",
  "info.cukes" % "cucumber-deps" % "0.6.3" % "test",
  "org.jruby" % "jruby-openssl" % "0.7.1" % "test",
  "org.jruby" % "jopenssl" % "0.7.1" % "test",
)


// ---------- End Core ----------

// ---------- Start Play ----------
lazy val clientPlay = Project(id="sso-client-play", base = file("./modules/play/"))
  .aggregate(clientPlayLibrary, clientPlayTesting)
  .settings(commonSettingsJava: _*)
  .settings(
    name := """sso-client-play""",
    publish := {},
    publishArtifact := false
  ).dependsOn(clientCore)

lazy val clientPlayLibrary = (project in file("./modules/play/library")).enablePlugins(PlayScala)
  .settings(commonSettings :_*)
  .settings(
    name := """sso-client-play""",
    libraryDependencies ++= playAppDeps ++ playTestDeps
  )
  .settings(repositorySettings :_*)
  .dependsOn(clientCore)

// Helper library for other apps' tests.
lazy val clientPlayTesting = (project in file("./modules/play/testing")).enablePlugins(PlayScala)
  .dependsOn(clientPlayLibrary)
  .settings(commonSettings :_*)
  .settings(
    name := """sso-client-play-testing""",
    libraryDependencies ++= playAppDeps ++ playTestDeps
  )
  .settings(repositorySettings :_*)

lazy val playAppDeps = Seq[ModuleID](
  guice,
  component("play-jdbc-api"),
  "xerces" % "xercesImpl" % "2.11.0",
  "xalan" % "xalan" % "2.7.1"
)

lazy val playTestDeps = Seq[ModuleID](
  jdbc,
  "com.typesafe.play" %% "play-iteratees" % "2.6.1",
  "com.typesafe.play" %% "play-iteratees-reactive-streams" % "2.6.1",
  "org.scalatest" %% "scalatest" % "3.0.3",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0",
  "org.scalacheck" %% "scalacheck" % "1.13.5",
  "org.mockito" % "mockito-all" % "1.10.19",
  "com.h2database" % "h2" % "1.4.193"
).map(_ % Test)

// https://bugs.elab.warwick.ac.uk/browse/SSO-1653
dependencyOverrides += "xml-apis" % "xml-apis" % "1.4.01"

// ---------- End Play ----------

// ---------- Start Servlet ----------

val cucumber = taskKey[Unit]("Runs cucumber integratoin tests")

lazy val clientServlet = Project(id="sso-client", base = file("./modules/servlet/"))
  .settings(commonSettingsJava: _*)
  .settings(
    name := """sso-client""",
    cucumber := Cucumber.run((fullClasspath in Test).value.files),
    libraryDependencies ++= servletDependencies,
  ).dependsOn(clientCore)

lazy val servletDependencies: Seq[ModuleID] = Seq(
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % Optional,
  "javax.servlet.jsp" % "javax.servlet.jsp-api" % "2.3.1" % Optional,
  "taglibs" % "standard" % "1.1.2" % Optional,
  "org.springframework" % "spring-jdbc" % s"$springVersion" % Optional,
  "org.slf4j" % "slf4j-simple" % "1.7.10" % Test,
  "uk.ac.warwick.util" % "warwickutils-cache" % s"$warwickUtilsVersion" % "test" classifier "tests",
  "junit" % "junit" % "4.12" % "test",
  "org.jmock" % "jmock-junit4" % "2.5.1" % "test",
  "org.hamcrest" % "hamcrest-library" % "1.3" % "test",
  "org.eclipse.jetty" % "jetty-server" % s"$jettyVersion" % "test",
  "org.eclipse.jetty" % "jetty-http" % s"$jettyVersion" % "test",
  "org.eclipse.jetty" % "jetty-io" % s"$jettyVersion" % "test",
  "org.eclipse.jetty" % "jetty-continuation" % s"$jettyVersion" % "test",
  "org.eclipse.jetty" % "jetty-websocket" % s"$jettyVersion" % "test",
  "org.eclipse.jetty" % "jetty-util" % s"$jettyVersion" % "test",
  "org.eclipse.jetty" % "jetty-webapp" % s"$jettyVersion" % "test",
  "org.springframework" % "spring-test" % s"$springVersion" % "test",
  "jmock" % "jmock-cglib" % "1.2.0" % "test",
  "xalan" % "xalan" % "2.7.0" % "test",
  "xerces" % "xercesImpl" % "2.6.0" % "test",
  "org.jruby" % "jruby-complete" % "1.4.0" % "test",
  "info.cukes" % "cucumber-deps" % "0.6.3" % "test",
  "org.jruby" % "jruby-openssl" % "0.7.1" % "test",
  "org.jruby" % "jopenssl" % "0.7.1" % "test"
) ++ clientCoreDeps

// ---------- End Servlet ----------

lazy val commonSettings = Seq(
  scalaVersion := "2.12.2",
  crossScalaVersions := Seq("2.11.8", "2.12.2"),
  publishMavenStyle := true,
  compileOrder := CompileOrder.ScalaThenJava, // maybe faster?

  organization := "uk.ac.warwick.sso",
  version := libraryVersion,
  exportJars := true,
  resolvers += WarwickNexus,
  resolvers += DefaultMavenRepository,
  resolvers += "oauth" at "http://oauth.googlecode.com/svn/code/maven"
)

lazy val commonSettingsJava = commonSettings ++ Seq(
  crossPaths := false, // stops SBT butchering the Maven artifactIds by appending Scala versions
  autoScalaLibrary := false // don't include the Scala library in the artifacts
)
