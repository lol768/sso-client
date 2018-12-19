import sbt._
import Defaults._

resolvers += "nexus" at "https://mvn.elab.warwick.ac.uk/nexus/content/groups/public"
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// For cleaning Ivy cache to get around snapshot problems
//  cleanCache "uk.ac.warwick.sso"
addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.2.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
