package warwick.sso

import com.google.inject.Inject
import com.google.inject.name.Named
import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import uk.ac.warwick.sso.client.SSOConfiguration

class SSOClientModuleTest extends PlaySpec with MockitoSugar {

  val module = new SSOClientModule

  // SSO assumes a few Play objects will be present - config and DB
  val playConf = Configuration.from(Map(
    "sso-client" -> Map (
      "cluster.enabled" -> false
    )
  ))
  
  "SSO Client module" should {
    "convert SSO config to Properties for Userlookup" in {
      val contents = new PropertiesConfiguration()
      contents.setProperty("cluster.enabled", true)
      contents.setProperty("cluster.db", "default")
      val ssoConfig = new SSOConfiguration(contents)
      val props = module.makeProps(ssoConfig)

      props.getProperty("userlookup.ssosUrl") must not be (null)
    }

    "create singletons" in {
      val app = GuiceApplicationBuilder(modules = Seq(module), configuration = playConf).build()
      app.injector.instanceOf(classOf[UserLookupService]) must be theSameInstanceAs app.injector.instanceOf(classOf[UserLookupService])
    }
    
    "return a generic GroupService" in {
      val app = GuiceApplicationBuilder(modules = Seq(module), configuration = playConf).build()
      val gs = app.injector.instanceOf(classOf[GroupService])
      gs.hasCache must be (true)
    }
    
    "return GroupService, both cached and uncached (using @Named)" in {
      val app = GuiceApplicationBuilder(modules = Seq(module), configuration = playConf).build()
      val testClass = app.injector.instanceOf(classOf[UsesUncached])
      testClass.ugs.hasCache must be (false)
      testClass.gs.hasCache must be (true)
    }
  }
}

class UsesUncached {
  @Inject @Named("uncached")
  val ugs: GroupService = null

  @Inject
  val gs: GroupService = null
}
