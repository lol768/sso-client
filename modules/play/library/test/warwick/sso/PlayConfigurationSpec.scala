package warwick.sso

import com.google.common.collect.Iterators
import com.typesafe.config.ConfigException
import org.apache.commons.configuration.Configuration
import org.scalatestplus.play.PlaySpec
import uk.ac.warwick.sso.client.SSOConfiguration
import warwick.sso.TestConfiguration.fromResource

class PlayConfigurationSpec extends PlaySpec {

  val conf = new PlayConfiguration(fromResource("spec.conf"))
  val ssoConf = new SSOConfiguration(conf)

  "PlayConfiguration" should {
    passSpec(conf)
  }

  "SSOConfiguration wrapping PlayConfiguration" should {
    passSpec(ssoConf)
  }

  def passSpec(conf: Configuration) : Unit = {
    "return a found value from getString" in {
      conf.getString("professor.name", "Gelb") must be ("Brian")
    }

    "return default value from getString" in {
      conf.getString("professor.blame", "Gelb") must be ("Gelb")
    }

    "throw error when no default provided" in {
      intercept[Exception] {
        conf.getString("professor.blame")
      }
    }

    "return a subset of config based on prefix" in {
      conf.subset("professor").getString("name") must be ("Brian")
    }

    "return booleans" in {
      conf.getBoolean("professor.billions") must be (true)
    }

    "return lists in Apache's special way" in {
      conf.getList("professor.friend.name").toArray mustBe Array("Dara", "Robin")
      conf.getList("professor.friend.speciality").toArray mustBe Array("Comedy", "Being Angry")
    }
  }
}
