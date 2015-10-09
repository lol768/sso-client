package warwick.sso

import javax.inject.{Inject, Provider}

import com.google.inject.{Provides, AbstractModule}
import org.apache.commons.lang.StringUtils
import uk.ac.warwick.sso.client.cache.spring.DatabaseUserCache
import uk.ac.warwick.sso.client.{SSOToken, SSOConfiguration, ShireCommand}
import uk.ac.warwick.sso.client.cache.{UserCacheItem, UserCache}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.cache.Cache

class GuiceModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ShireCommandProvider])
    bind(classOf[AssertionConsumer])

    bind(classOf[UserCache]).asEagerSingleton()
  }

  @Provides
  def userCache(config: SSOConfiguration): UserCache = {
    if (config.getBoolean("cluster.enabled", false)) {
      val dbCache = new JdbcUserCache()
      if (StringUtils.isNotEmpty(keyName)) {
        dbCache.setKeyName(keyName)
      }

      dbCache.setDataSource(getDataSource(dsName))
    } else {

    }
  }
}




