package warwick.sso

import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Configuration, Environment}

/**
 * Test of the SSOClientModule. It should look under
 * a section of Play conf called sso-client.
 *
 * `cluster.db` can be set to choose a database name
 * other than default for accessing the objectcache table.
 */
class IntegrationSpec extends PlaySpec with Results {

  "SSOClient" should {
    "configure from sso-client section of Play config" in {
      val config = Configuration.load(Environment.simple(), Map(
          "sso-client.cluster.enabled" -> "true",
          "sso-client.cluster.db" -> "photos"
        ) ++ inMemoryDatabase(name="photos"))

      // Build a fake Play app, to spin up the DB and modules.
      val app = new GuiceApplicationBuilder()
        .in(Environment.simple())
        .loadConfig(config)
        .overrides(new SSOClientModule)
        .build()

      val assertionConsumer = app.injector.instanceOf[AssertionConsumer]

      val client = app.injector.instanceOf[SsoClient]

    }
  }

}
