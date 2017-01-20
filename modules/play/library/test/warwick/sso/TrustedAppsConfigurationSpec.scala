package warwick.sso

import org.scalatestplus.play.PlaySpec
import uk.ac.warwick.sso.client.SSOConfiguration
import uk.ac.warwick.sso.client.trusted.SSOConfigTrustedApplicationsManager
import warwick.sso.TestConfiguration.fromResource

class TrustedAppsConfigurationSpec extends PlaySpec {

  val conf = new PlayConfiguration(fromResource("sample.conf"))
  val ssoConf = new SSOConfiguration(conf.subset("sso-client"))

  val ta = new SSOConfigTrustedApplicationsManager(ssoConf)

  "Play SSO configuration" should {
    "interpret trusted apps config" in {
      ta.getCurrentApplication.getProviderID mustBe "urn:example.com:start:service"
      ta.getTrustedApplication("urn:start.warwick.ac.uk:portal:service") mustNot be (null)
    }
  }


}
