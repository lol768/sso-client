def libraryVersion = "2.63-SNAPSHOT" // propagates downwards
def warwickUtilsVersion = "20190125"
def jettyVersion = "8.2.0.v20160908"
def springVersion = "4.3.21.RELEASE"

lazy val root = (project in file("."))
  .aggregate(clientCore, clientPlay, clientServlet)
  .settings(
    publish := {},
    publishArtifact := false
  )

// ---------- Start Core ----------

lazy val clientCore = (project in file("./modules/core"))
  .settings(commonSettingsJava: _*)
  .settings(
    name := "sso-client-core",
    libraryDependencies ++= clientCoreDeps,
    resourceGenerators in Compile += Def.task {
      val file = (resourceManaged in Compile).value / "ssoclient.version"
      val contents = "version=%s".format(version.value)
      IO.write(file, contents)
      Seq(file)
    }.taskValue
  )

lazy val clientCoreDeps = Seq(
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % Optional,
  "javax.inject" % "javax.inject" % "1",
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "xfire" % "opensaml" % "1.0.1",
  "xalan" % "xalan" % "2.7.2",
  "xerces" % "xercesImpl" % "2.12.0",
  "org.apache.santuario" % "xmlsec" % "1.5.8" exclude("javax.servlet", "servlet-api"),
  "taglibs" % "standard" % "1.1.2" % Optional ,
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "org.apache.httpcomponents" % "httpmime" % "4.4.1",
  "commons-configuration" % "commons-configuration" % "1.10"
    exclude("servletapi", "servletapi")
    exclude("commons-beanutils", "commons-beanutils")
    exclude("commons-beanutils", "commons-beanutils-bean-collections")
    exclude("commons-beanutils", "commons-beanutils-core"),
  "net.oauth.core" % "oauth" % "20090825",
  "net.oauth.core" % "oauth-provider" % "20090531",
  "net.oauth.core" % "oauth-httpclient4" % "20090913",
  "org.apache.httpcomponents" % "httpasyncclient" % "4.1.2",
  "org.apache.httpcomponents" % "httpcore" % "4.4.6",
  "uk.ac.warwick.util" % "warwickutils-cache" % warwickUtilsVersion,
  "uk.ac.warwick.util" % "warwickutils-core" % warwickUtilsVersion,
  "uk.ac.warwick.util" % "warwickutils-web" % warwickUtilsVersion
    exclude("uk.ac.warwick.sso", "sso-client"),
  "org.bouncycastle" % "bcprov-jdk15on" % "1.60",
  "commons-collections" % "commons-collections" % "3.2.2",
  "net.sf.ehcache" % "ehcache" % "2.9.0" % Optional ,
  "net.spy" % "spymemcached" % "2.10.6" % Optional ,

  "org.slf4j" % "slf4j-simple" % "1.7.10" % Test,
  "uk.ac.warwick.util" % "warwickutils-cache" % warwickUtilsVersion % Test,
  "junit" % "junit" % "4.12" % Test,
  "org.jmock" % "jmock-junit4" % "2.5.1" % Test,
  "org.hamcrest" % "hamcrest-library" % "1.3" % Test,
  "org.eclipse.jetty" % "jetty-server" % jettyVersion % Test,
  "org.eclipse.jetty" % "jetty-http" % jettyVersion % Test,
  "org.eclipse.jetty" % "jetty-io" % jettyVersion % Test,
  "org.eclipse.jetty" % "jetty-continuation" % jettyVersion % Test,
  "org.eclipse.jetty" % "jetty-websocket" % jettyVersion % Test,
  "org.eclipse.jetty" % "jetty-util" % jettyVersion % Test,
  "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % Test,
  "org.springframework" % "spring-test" % springVersion % Test,
  "jmock" % "jmock-cglib" % "1.2.0" % Test,
  "org.jruby" % "jruby-complete" % "1.4.0" % Test,
  "info.cukes" % "cucumber-deps" % "0.6.3" % Test,
  "org.jruby" % "jruby-openssl" % "0.7.1" % Test,
  "org.jruby" % "jopenssl" % "0.7.1" % Test,
)


// ---------- End Core ----------

// ---------- Start Play ----------
lazy val clientPlay = (project in file("./modules/play"))
  .aggregate(clientPlayLibrary, clientPlayTesting)
  .settings(
    publish := {},
    publishArtifact := false
  )

lazy val clientPlayLibrary = (project in file("./modules/play/library"))
  .enablePlugins(PlayScala)
  .settings(commonSettings :_*)
  .settings(
    name := "sso-client-play",
    libraryDependencies ++= playAppDeps ++ playTestDeps
  )
  .dependsOn(clientCore)

// Helper library for other apps' tests.
lazy val clientPlayTesting = (project in file("./modules/play/testing"))
  .enablePlugins(PlayScala)
  .dependsOn(clientPlayLibrary)
  .settings(commonSettings :_*)
  .settings(
    name := "sso-client-play-testing",
    libraryDependencies ++= playAppDeps ++ playTestDeps
  )

lazy val playAppDeps = Seq[ModuleID](
  guice,
  component("play-jdbc-api"),

  // https://snyk.io/vuln/SNYK-JAVA-COMFASTERXMLJACKSONCORE-72445
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.11.3"
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

excludeDependencies += "commons-logging" % "commons-logging"

// ---------- End Play ----------

// ---------- Start Servlet ----------

val cucumber = taskKey[Unit]("Runs cucumber integration tests")

lazy val clientServlet = (project in file("./modules/servlet"))
  .settings(commonSettingsJava: _*)
  .settings(
    name := "sso-client",
    cucumber := Cucumber.run((fullClasspath in Test).value.files),
    libraryDependencies ++= servletDependencies,
  ).dependsOn(clientCore)

lazy val servletDependencies: Seq[ModuleID] = clientCoreDeps ++ Seq(
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % Optional,
  "javax.servlet.jsp" % "javax.servlet.jsp-api" % "2.3.1" % Optional,
  "taglibs" % "standard" % "1.1.2" % Optional,
  "org.springframework" % "spring-jdbc" % springVersion % Optional,
  "org.slf4j" % "slf4j-simple" % "1.7.10" % Test,
  
  "uk.ac.warwick.util" % "warwickutils-cache" % warwickUtilsVersion % Test classifier "tests",
  "junit" % "junit" % "4.12" % Test,
  "org.jmock" % "jmock-junit4" % "2.5.1" % Test,
  "org.hamcrest" % "hamcrest-library" % "1.3" % Test,
  "org.eclipse.jetty" % "jetty-server" % jettyVersion % Test,
  "org.eclipse.jetty" % "jetty-http" % jettyVersion % Test,
  "org.eclipse.jetty" % "jetty-io" % jettyVersion % Test,
  "org.eclipse.jetty" % "jetty-continuation" % jettyVersion % Test,
  "org.eclipse.jetty" % "jetty-websocket" % jettyVersion % Test,
  "org.eclipse.jetty" % "jetty-util" % jettyVersion % Test,
  "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % Test,
  "org.springframework" % "spring-test" % springVersion % Test,
  "jmock" % "jmock-cglib" % "1.2.0" % Test,
  "org.jruby" % "jruby-complete" % "1.4.0" % Test,
  "info.cukes" % "cucumber-deps" % "0.6.3" % Test,
  "org.jruby" % "jruby-openssl" % "0.7.1" % Test,
  "org.jruby" % "jopenssl" % "0.7.1" % Test,
)

// ---------- End Servlet ----------

lazy val commonSettings = Seq(
  scalaVersion := "2.12.8",
  crossScalaVersions := Seq("2.12.8"),
  publishMavenStyle := true,

  organization := "uk.ac.warwick.sso",
  version := libraryVersion,
  resolvers += WarwickPublicNexus,
  resolvers += DefaultMavenRepository,
  resolvers += "oauth" at "http://oauth.googlecode.com/svn/code/maven",

  // Fix publishing on SBT 1.x
  // https://github.com/sbt/sbt/issues/3570
  updateOptions := updateOptions.value.withGigahorse(false)
) ++ publicRepositorySettings

lazy val commonSettingsJava = commonSettings ++ Seq(
  crossScalaVersions := Nil,
  crossPaths := false, // stops SBT butchering the Maven artifactIds by appending Scala versions
  autoScalaLibrary := false // don't include the Scala library in the artifacts
)

ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishMavenStyle := true

