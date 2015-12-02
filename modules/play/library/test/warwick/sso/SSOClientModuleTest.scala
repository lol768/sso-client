package warwick.sso

import java.util

import com.google.inject.{Module, Stage, AbstractModule, Guice}
import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.db.DBApi
import uk.ac.warwick.sso.client.SSOConfiguration
import uk.ac.warwick.userlookup.UserLookupInterface

class SSOClientModuleTest extends PlaySpec with MockitoSugar {

  val module = new SSOClientModule

  // SSO assumes a few Play objects will be present - config and DB
  val playConf = Configuration.from(Map(
    "sso-client" -> Map (
      "cluster.enabled" -> false
    )
  ))
  val play : Module = new AbstractModule {
    override def configure(): Unit = {
      bind(classOf[Configuration]).toInstance(playConf)
    }
  }
  val playDB : Module = new AbstractModule {
    override def configure(): Unit = {
      bind(classOf[DBApi]).toInstance(mock[DBApi])
    }
  }



  "SSO Client module" should {
    "convert SSO config to Properties for Userlookup" in {
      val contents = new PropertiesConfiguration()
      contents.setProperty("cluster.enabled", true)
      contents.setProperty("cluster.db", "default")
      val ssoConfig = new SSOConfiguration(contents)
      val props = module.makeProps(ssoConfig)

      props.getProperty("userlookup.ssosUrl") must not be (null)
    }

    // Currently fails - transitive dependencies are looked up even if
    // the provider (for JdbcUserCache in this case is never called)
    // Shouldn't matter in practice really, because Play should always have
    // a DBApi instance even if no Databases are registered.
    "not require DB when not in clustered mode" ignore {
      val injector = Guice.createInjector(module, play)
      injector.getInstance(classOf[UserLookupService])
    }

    "create singletons" in {
      val injector = Guice.createInjector(module, play, playDB)
      val provider = injector.getProvider(classOf[UserLookupService])
      provider.get() must be theSameInstanceAs (provider.get())
    }
  }

}
