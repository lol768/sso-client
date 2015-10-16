package warwick.sso

import org.scalatestplus.play._
import uk.ac.warwick.sso.client.SSOConfiguration
import uk.ac.warwick.sso.client.cache.UserCache
import warwick.sso.TestConfiguration._

class AssertionConsumerSpec extends PlaySpec {

  val conf = new PlayConfiguration(fromResource("spec.conf"))

  "consumer" should {
    "win" in {
      val config = new SSOConfiguration(conf)
//      val consumer = new AssertionConsumer(config, userCache, userIdCache)
    }
  }

}
