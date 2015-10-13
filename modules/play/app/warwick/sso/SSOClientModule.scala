package warwick.sso

import javax.inject._

import com.google.inject.name.Names
import com.google.inject.{Exposed, PrivateModule, Provides, AbstractModule}
import org.apache.commons.lang.StringUtils
import play.api.{Configuration, Environment}
import play.api.db.{DBApi, Database}
import uk.ac.warwick.sso.client._
import uk.ac.warwick.sso.client.cache.{InMemoryUserCache, UserCacheItem, UserCache}
import uk.ac.warwick.sso.client.core.{OnCampusServiceImpl, OnCampusService}
import uk.ac.warwick.userlookup.{UserLookupInterface, UserLookup, User}
import uk.ac.warwick.util.cache.{Caches, Cache}

/**
 * Guice module for setting up all the relevant SSO Client
 * objects. As this is a private module, most of the beans
 * are not available to an app using this module except where
 * they are explicitly exposed.
 *
 * Beans you might be interested in:
 */
class SSOClientModule extends PrivateModule {
  override def configure(): Unit = {
    bind(classOf[AssertionConsumer])
    bind(classOf[SsoClient]).to(classOf[SsoClientImpl])
    bind(classOf[SSOClientHandler]).to(classOf[SSOClientHandlerImpl])
    bind(classOf[OnCampusService]).to(classOf[OnCampusServiceImpl])

    // public beans
    expose(classOf[AssertionConsumer])
    expose(classOf[SsoClient])
  }

  @Provides
  @Exposed
  def userlookup() : UserLookupInterface = new UserLookup()

  @Provides @Named("SSOClientDB")
  def db(ssoConfig: SSOConfiguration, api: DBApi): Database =
    api.database(ssoConfig.getString("cluster.db", "default"))


  @Exposed
  @Provides
  def config(conf: Configuration): SSOConfiguration = {
    new SSOConfiguration(new PlayConfiguration(
      conf.getConfig("sso-client").getOrElse(throw new RuntimeException("Expecting sso-client section of config"))
    ))
  }

  @Provides
  def aaFetcher(conf: SSOConfiguration): AttributeAuthorityResponseFetcher =
    new AttributeAuthorityResponseFetcherImpl(conf)

  @Provides
  def shireCommand(
      config: SSOConfiguration,
      userCache: UserCache,
      userIdCache: Cache[String, User]
      ) : ShireCommand =
    new ShireCommand(config, userCache, userIdCache)

  @Singleton
  @Provides
  @Named("InMemory")
  def inMemoryCache(conf: SSOConfiguration): UserCache = new InMemoryUserCache(conf)

  @Provides
  def userIdCache(conf: SSOConfiguration): Cache[String, User] =
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




