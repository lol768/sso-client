package warwick.sso

import com.google.inject.Guice
import org.scalatestplus.play.PlaySpec

class IntegrationSpec extends PlaySpec {

  "SSOClient" should {
    "configure with minimal effort" in {
      val injector = Guice.createInjector(new GuiceModule)
      val assertionConsumer = injector.getInstance(classOf[AssertionConsumer])
    }
  }

}
