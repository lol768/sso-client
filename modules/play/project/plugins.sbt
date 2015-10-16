import sbt._
import Defaults._

// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += "nexus" at "https://mvn.elab.warwick.ac.uk/nexus/content/groups/public"
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

addSbtPlugin("uk.ac.warwick" % "play-warwick" % "1.0-SNAPSHOT")


// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0")