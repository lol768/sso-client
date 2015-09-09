import sbt.Keys._

lazy val commonSettings = Seq(
    version := "1.0",
    scalaVersion := "2.11.6",
    // This speeds things up if you don't have Java code depending on Scala code
    //compileOrder := CompileOrder.JavaThenScala
    //compileOrder := CompileOrder.ScalaThenJava,
    javacOptions ++= Seq("-source", "6", "-target", "6")
  )

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := """sso-client""",
    autoScalaLibrary := false,
    libraryDependencies ++= allDependencies ++ testDependencies,
    parallelExecution in Test := false
  )

val springVersion = "3.1.0.RELEASE"
val jettyVersion = "7.0.1.v20091125"
val utilsVersion = "20140319-1227"
//val utilsVersion = "20150610"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
resolvers += DefaultMavenRepository 
resolvers += "nexus" at "https://mvn.elab.warwick.ac.uk/nexus/content/groups/public"
resolvers += "oauth" at "http://oauth.googlecode.com/svn/code/maven"

val testDependencies = Seq(
  "com.novocode" % "junit-interface" % "0.8",
  "junit"             % "junit"           % "4.12", 
  "jetty" % "org.mortbay.jetty" % "5.1.4",
  "org.eclipse.jetty" % "jetty-server" % jettyVersion,
  "org.eclipse.jetty" % "jetty-http" % jettyVersion,
  "org.eclipse.jetty" % "jetty-io" % jettyVersion,
  "org.eclipse.jetty" % "jetty-continuation" % jettyVersion,
  "org.eclipse.jetty" % "jetty-websocket" % jettyVersion,
  "org.eclipse.jetty" % "jetty-util" % jettyVersion,
  "uk.ac.warwick.util" % "warwickutils-test" % utilsVersion
    excludeAll(
      Seq("core","cache","web","httpclient3","httpclient4","mail","queue").map(utl => ExclusionRule(name="warwickutils-"+utl )) : _*
    ),
  "org.jmock" % "jmock-junit4" % "2.5.1",
  "jmock" % "jmock-cglib" % "1.2.0",
  "org.springframework" % "spring-test" % springVersion,
  "org.jruby" % "jruby-complete" % "1.4.0",
  "org.jruby" % "jruby-openssl" % "0.7.1",
  "org.jruby" % "jopenssl" % "0.7.1",
  "info.cukes" % "cucumber-deps" % "0.6.3"
).map(_ % "test")

val allDependencies = Seq(
  "commons-configuration" % "commons-configuration" % "1.1",
  "javax.servlet" % "servlet-api" % "2.5",
  "javax.servlet.jsp" % "javax.servlet.jsp-api" % "2.2.1",
  "log4j" % "log4j" % "1.2.17",
  "net.oauth.core" % "oauth" % "20090825",
  "net.oauth.core" % "oauth-httpclient3" % "20090617",
  "net.oauth.core" % "oauth-provider" % "20090531",
  "net.sf.ehcache" % "ehcache-core" % "1.7.0",
  "net.spy" % "spymemcached" % "2.10.6",
  "org.apache.httpcomponents" % "httpcore" % "4.3.1",
  "org.apache.httpcomponents" % "httpmime" % "4.3.1",
  "org.apache.santuario" % "xmlsec" % "1.4.3",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.52",
  "org.springframework" % "spring-core" % springVersion,
  "org.springframework" % "spring-jdbc" % springVersion,
  "taglibs" % "standard" % "1.1.2",
  "uk.ac.warwick.util" % "warwickutils-cache" % utilsVersion,
  "uk.ac.warwick.util" % "warwickutils-core" % utilsVersion,
  "xalan" % "xalan" % "2.7.0",
  "xerces" % "xercesImpl" % "2.6.0", 
  "xfire" % "opensaml" % "1.0.1" 
  )

dependencyOverrides += "org.apache.httpcomponents" % "httpcore" % "4.3.1"
dependencyOverrides += "org.apache.httpcomponents" % "httpmime" % "4.3.1"


//fork in run := true
