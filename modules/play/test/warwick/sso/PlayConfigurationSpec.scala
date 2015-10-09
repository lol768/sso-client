package warwick.sso

import com.google.common.collect.Iterators
import org.scalatestplus.play.PlaySpec
import warwick.sso.TestConfiguration.fromResource

class PlayConfigurationSpec extends PlaySpec {

  val conf = new PlayConfiguration(fromResource("spec.conf"))

  "PlayConfigurationSpec" should {

    "return a found value from getString" in {
      conf.getString("professor.name", "Gelb") must be ("Brian")
    }

    "return default value from getString" in {
      conf.getString("professor.blame", "Gelb") must be ("Gelb")
    }

    "throw error when no default provided" in {
      intercept[NoSuchElementException] {
        conf.getString("professor.blame")
      }
    }

    "return a subset of config based on prefix" in {
      conf.subset("professor").getString("name") must be ("Brian")
    }

  }
}
