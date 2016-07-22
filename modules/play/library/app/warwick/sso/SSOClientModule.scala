package warwick.sso

import java.util.Properties
import javax.inject._

import com.google.inject.{Exposed, PrivateModule, Provides, Scopes}
import play.api.Configuration
import play.api.db.{DBApi, Database}
import uk.ac.warwick.sso.client._
import uk.ac.warwick.sso.client.cache.{InMemoryUserCache, UserCache}
import uk.ac.warwick.sso.client.core.{OnCampusService, OnCampusServiceImpl}
import uk.ac.warwick.sso.client.trusted.{SSOConfigTrustedApplicationsManager, TrustedApplicationHandler, TrustedApplicationHandlerImpl, TrustedApplicationsManager}
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
    bind(classOf[AssertionConsumer]).in(Scopes.SINGLETON)
    bind(classOf[SSOClient]).to(classOf[SSOClientImpl]).in(Scopes.SINGLETON)
    bind(classOf[SSOClientHandler]).to(classOf[SSOClientHandlerImpl]).in(Scopes.SINGLETON)
    bind(classOf[TrustedApplicationHandler]).to(classOf[TrustedApplicationHandlerImpl]).in(Scopes.SINGLETON)
    bind(classOf[OnCampusService]).to(classOf[OnCampusServiceImpl]).in(Scopes.SINGLETON)
    bind(classOf[UserLookupService]).in(Scopes.SINGLETON)
    bind(classOf[GroupService]).in(Scopes.SINGLETON)
    bind(classOf[LogoutController]).in(Scopes.SINGLETON)
    bind(classOf[BasicAuth]).to(classOf[BasicAuthImpl]).in(Scopes.SINGLETON)
    bind(classOf[TrustedApplicationsManager])
      .toConstructor(classOf[SSOConfigTrustedApplicationsManager].getConstructor(classOf[SSOConfiguration]))
      .in(Scopes.SINGLETON)

    // public beans
    expose(classOf[AssertionConsumer])
    expose(classOf[LogoutController])
    expose(classOf[SSOClient])
    expose(classOf[UserLookupService])
    expose(classOf[BasicAuth])
    expose(classOf[TrustedApplicationsManager])
    expose(classOf[UserLookupInterface])
    expose(classOf[GroupService])

    // things that probably shouldn't be public, but if you're good...
    expose(classOf[UserCache])
  }

  @Singleton
  @Provides
  def userlookup(ssoConfig: SSOConfiguration): UserLookupInterface = {
    UserLookup.setConfigProperties(makeProps(ssoConfig))
    new UserLookup()
  }

  @Singleton
  @Provides
  def groupService(userLookup: UserLookupInterface) =
    userLookup.getGroupService

  private[sso] def makeProps(ssoConfig: SSOConfiguration): Properties = {
    val props = new Properties()
    for (key <- ssoConfig.getKeys.asScala.asInstanceOf[Iterator[String]]) {
      props.setProperty(key, ssoConfig.getProperty(key).toString)
    }
    props
  }

  @Provides
  @Named("SSOClientDB")
  def db(ssoConfig: SSOConfiguration, api: DBApi): Database =
    api.database(ssoConfig.getString("cluster.db", "default"))

  @Singleton
  @Exposed
  @Provides
  def config(conf: Configuration): SSOConfiguration = {
    new SSOConfiguration(new PlayConfiguration(
      conf.getConfig("sso-client")
        .getOrElse(throw new RuntimeException("SSO Client configuration should be under an 'sso-client' section of your Play config."))
    ))
  }

  @Singleton
  @Provides
  def aaFetcher(conf: SSOConfiguration): AttributeAuthorityResponseFetcher =
    new AttributeAuthorityResponseFetcherImpl(conf)

  @Provides
  def shireCommand(
    config: SSOConfiguration,
    userCache: UserCache,
    userIdCache: Cache[String, uk.ac.warwick.userlookup.User]
  ): ShireCommand =
    new ShireCommand(config, userCache, userIdCache)

  @Singleton
  @Provides
  @Named("InMemory")
  def inMemoryCache(conf: SSOConfiguration): UserCache = new InMemoryUserCache(conf)

  @Singleton
  @Provides
  def userIdCache(conf: SSOConfiguration): Cache[String, uk.ac.warwick.userlookup.User] =
    Caches.newCache(UserLookup.USER_CACHE_NAME, null, 0, Caches.CacheStrategy.valueOf(conf.getString("ssoclient.cache.strategy")))

  @Singleton
  @Provides
  def userCache(
    config: SSOConfiguration,
    jdbcCache: Provider[JdbcUserCache],
    @Named("InMemory") memoryCache: Provider[UserCache]
  ): UserCache =
    if (config.getBoolean("cluster.enabled", false)) jdbcCache.get()
    else memoryCache.get()

}




