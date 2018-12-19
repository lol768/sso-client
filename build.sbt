
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
  "javax.servlet" % "javax.servlet-api" % "3.1.0",
  "javax.inject" % "javax.inject" % "1" % Optional,
  "org.slf4j" % "slf4j-api" % "1.7.10" % Optional,
  "org.slf4j" % "log4j-over-slf4j" % "1.7.10" % Optional,
  "xfire" % "opensaml" % "1.0.1" % Optional,
  "org.apache.santuario" % "xmlsec" % "1.4.3" % Optional,
  "taglibs" % "standard" % "1.1.2",
  "org.apache.httpcomponents" % "httpclient" % "4.5.2" % Optional,
  "org.apache.httpcomponents" % "httpmime" % "4.4.1" % Optional,
  "commons-configuration" % "commons-configuration" % "1.1" % Optional,
  "net.oauth.core" % "oauth" % "20090825" % Optional,
  "net.oauth.core" % "oauth-provider" % "20090531" % Optional,
  "net.oauth.core" % "oauth-httpclient4" % "20090913" % Optional,
  "org.apache.httpcomponents" % "httpasyncclient" % "4.1.2" % Optional,
  "org.apache.httpcomponents" % "httpcore" % "4.4.6" % Optional,
  "uk.ac.warwick.util" % "warwickutils-cache" % s"$warwickUtilsVersion" % Optional,
  "uk.ac.warwick.util" % "warwickutils-core" % s"$warwickUtilsVersion" % Optional,
  "uk.ac.warwick.util" % "warwickutils-web" % s"$warwickUtilsVersion" % Optional,
  "org.bouncycastle" % "bcprov-jdk15on" % "1.60" % Optional,
  "net.sf.ehcache" % "ehcache" % "2.9.0",
  "net.spy" % "spymemcached" % "2.10.6",
  "org.slf4j" % "slf4j-simple" % "1.7.10" % Optional,
  "uk.ac.warwick.util" % "warwickutils-cache" % s"$warwickUtilsVersion" % Optional,
  "junit" % "junit" % "4.12" % Optional,
  "org.jmock" % "jmock-junit4" % "2.5.1" % Optional,
  "org.hamcrest" % "hamcrest-library" % "1.3" % Optional,
  "org.eclipse.jetty" % "jetty-server" % s"$jettyVersion" % Optional,
  "org.eclipse.jetty" % "jetty-http" % s"$jettyVersion" % Optional,
  "org.eclipse.jetty" % "jetty-io" % s"$jettyVersion" % Optional,
  "org.eclipse.jetty" % "jetty-continuation" % s"$jettyVersion" % Optional,
  "org.eclipse.jetty" % "jetty-websocket" % s"$jettyVersion" % Optional,
  "org.eclipse.jetty" % "jetty-util" % s"$jettyVersion" % Optional,
  "org.eclipse.jetty" % "jetty-webapp" % s"$jettyVersion" % Optional,
  "org.springframework" % "spring-test" % s"$springVersion" % Optional,
  "jmock" % "jmock-cglib" % "1.2.0" % Optional,
  "xalan" % "xalan" % "2.7.0" % Optional,
  "xerces" % "xercesImpl" % "2.6.0" % Optional,
  "org.jruby" % "jruby-complete" % "1.4.0" % Optional,
  "info.cukes" % "cucumber-deps" % "0.6.3" % Optional,
  "org.jruby" % "jruby-openssl" % "0.7.1" % Optional,
  "org.jruby" % "jopenssl" % "0.7.1" % Optional
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
  "uk.ac.warwick.sso" % "sso-client-core" % libraryVersion,
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

lazy val clientServlet = Project(id="sso-client", base = file("./modules/servlet/"))
  .settings(commonSettingsJava: _*)
  .settings(
    name := """sso-client""",
    libraryDependencies ++= servletDependencies
  ).dependsOn(clientCore)

lazy val servletDependencies = Seq(
  "org.jruby" % "jruby-complete" % "1.4.0",
  "info.cukes" % "cucumber-deps" % "0.6.3",
  "org.jruby" % "jruby-openssl" % "0.7.1",
  "org.jruby" % "jopenssl" % "0.7.1",
  "org.jruby" % "jruby-complete" % "1.4.0",
  "info.cukes" % "cucumber-deps" % "0.6.3",
  "org.jruby" % "jruby-openssl" % "0.7.1",
  "org.jruby" % "jopenssl" % "0.7.1",
  "uk.ac.warwick.sso" % "sso-client-core" % s"$libraryVersion",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % Optional,
  "javax.servlet.jsp" % "javax.servlet.jsp-api" % "2.3.1" % Optional,
  "taglibs" % "standard" % "1.1.2" % Optional,
  "org.springframework" % "spring-jdbc" % s"$springVersion" % Optional,
  "org.slf4j" % "slf4j-simple" % "1.7.10" % Test,
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
  "junit" % "junit" % "4.12" % Test
)

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
