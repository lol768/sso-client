package warwick.sso

import java.util.Properties
import javax.inject._

import com.google.inject.{Exposed, PrivateModule, Provides}
import play.api.Configuration
import play.api.db.{DBApi, Database}
import uk.ac.warwick.sso.client._
import uk.ac.warwick.sso.client.cache.{InMemoryUserCache, UserCache}
import uk.ac.warwick.sso.client.core.{OnCampusService, OnCampusServiceImpl}
import uk.ac.warwick.userlookup.{UserLookup, UserLookupInterface}
import uk.ac.warwick.util.cache.{Cache, Caches}

import scala.collection.JavaConverters._

/**
 * Guice module for setting up all the relevant SSO Client
 * objects. As this is a private module, most of the beans
 * are not available to an app using this module except where
 * they are explicitly exposed.
 */
class SSOClientModule extends PrivateModule {
  override def configure(): Unit = {
    bind(classOf[AssertionConsumer])
    bind(classOf[SSOClient]).to(classOf[SSOClientImpl])
    bind(classOf[SSOClientHandler]).to(classOf[SSOClientHandlerImpl])
    bind(classOf[OnCampusService]).to(classOf[OnCampusServiceImpl])
    bind(classOf[UserLookupService])
    bind(classOf[LogoutController])
    bind(classOf[BasicAuth]).to(classOf[BasicAuthImpl])

    // public beans
    expose(classOf[AssertionConsumer])
    expose(classOf[LogoutController])
    expose(classOf[SSOClient])
    expose(classOf[UserLookupService])
    expose(classOf[BasicAuth])
  }

  @Provides
  def userlookup(ssoConfig: SSOConfiguration) : UserLookupInterface = {
    UserLookup.setConfigProperties(makeProps(ssoConfig))
    new UserLookup()
  }

  private[sso] def makeProps(ssoConfig: SSOConfiguration): Properties = {
    val props = new Properties()
    for (key <- ssoConfig.getKeys.asScala.asInstanceOf[Iterator[String]]) {
      props.setProperty(key, ssoConfig.getProperty(key).toString)
    }
    props
  }

  @Provides @Named("SSOClientDB")
  def db(ssoConfig: SSOConfiguration, api: DBApi): Database =
    api.database(ssoConfig.getString("cluster.db", "default"))


  @Exposed
  @Provides
  def config(conf: Configuration): SSOConfiguration = {
    new SSOConfiguration(new PlayConfiguration(
      conf.getConfig("sso-client")
        .getOrElse(throw new RuntimeException("SSO Client configuration should be under an 'sso-client' section of your Play config."))
    ))
  }

  @Provides
  def aaFetcher(conf: SSOConfiguration): AttributeAuthorityResponseFetcher =
    new AttributeAuthorityResponseFetcherImpl(conf)

  @Provides
  def shireCommand(
      config: SSOConfiguration,
      userCache: UserCache,
      userIdCache: Cache[String, uk.ac.warwick.userlookup.User]
      ) : ShireCommand =
    new ShireCommand(config, userCache, userIdCache)

  @Singleton
  @Provides
  @Named("InMemory")
  def inMemoryCache(conf: SSOConfiguration): UserCache = new InMemoryUserCache(conf)

  @Provides
  def userIdCache(conf: SSOConfiguration): Cache[String, uk.ac.warwick.userlookup.User] =
    Caches.newCache(UserLookup.USER_CACHE_NAME, null, 0, Caches.CacheStrategy.valueOf(conf.getString("ssoclient.cache.strategy")))

  @Provides
  def userCache(
       config: SSOConfiguration,
       jdbcCache: Provider[JdbcUserCache],
       @Named("InMemory") memoryCache: Provider[UserCache]
    ) : UserCache =
    if (config.getBoolean("cluster.enabled", false)) jdbcCache.get()
    else memoryCache.get()

}




