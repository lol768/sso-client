package warwick.sso

import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatestplus.play.PlaySpec
import uk.ac.warwick.sso.client.SSOConfiguration

class SSOClientModuleTest extends PlaySpec {

  val module = new SSOClientModule

  "SSO Client module" should {
    "convert SSO config to Properties for Userlookup" in {
      val contents = new PropertiesConfiguration()
      contents.setProperty("cluster.enabled", true)
      contents.setProperty("cluster.db", "default")
      val ssoConfig = new SSOConfiguration(contents)
      module.makeProps(ssoConfig)
    }
  }

}
