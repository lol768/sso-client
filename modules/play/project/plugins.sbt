import sbt._
import Defaults._

resolvers += "nexus" at "https://mvn.elab.warwick.ac.uk/nexus/content/groups/public"
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

addSbtPlugin("uk.ac.warwick" % "play-warwick" % "0.1")

// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.0")

// For cleaning Ivy cache to get around snapshot problems
//  cleanCache "uk.ac.warwick.sso"
addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.1.0")
