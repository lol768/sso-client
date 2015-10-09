package warwick.sso

import play.api.{Mode, Environment, Configuration}


object TestConfiguration {
  def fromResource(name: String): Configuration = Configuration.load(
    Environment.simple(),
    Map("config.resource" -> name)
  )
}
