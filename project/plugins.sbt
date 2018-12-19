import sbt._
import Defaults._

resolvers += "nexus" at "https://mvn.elab.warwick.ac.uk/nexus/content/groups/public"
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

addSbtPlugin("uk.ac.warwick" % "play-warwick" % "0.7")

// don't think we ever make shaded libs
// addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.9")


// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.1")
