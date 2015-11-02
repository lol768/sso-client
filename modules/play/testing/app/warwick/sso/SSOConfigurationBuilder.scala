package warwick.sso

import org.apache.commons.configuration.PropertiesConfiguration
import uk.ac.warwick.sso.client.SSOConfiguration


class SSOConfigurationBuilder() {

  private var props = Map[String,String](
    "logout.location" -> "https://sp.example.com/sso/logout",
    "mode" -> "new",
    "cluster.enabled" -> "true",
    "shire.sscookie.name" -> "SSC-Example",
    "shire.sscookie.domain" -> "sp.example.com",
    "shire.sscookie.path" -> "/",
    "shire.location" -> "https://sp.example.com/sso/acs",
    "shire.providerid" -> "urn:example.com:sp:service",
    "credentials.certificate" -> "file:///tmp/example.crt",
    "credentials.key" -> "file:///tmp/example.key",
    "credentials.chain" -> "file:///tmp/example-chain.pem"
  )

  private def add(key: String, value: String): this.type = {
    props += key -> value
    this
  }

  def build() : SSOConfiguration = {
    val conf = new PropertiesConfiguration()
    for ((key, value) <- props) {
      conf.addProperty(key, value)
    }
    new SSOConfiguration(conf)
  }

}
