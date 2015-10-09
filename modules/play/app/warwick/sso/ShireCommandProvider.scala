package warwick.sso

import javax.inject.{Provider, Inject}

import uk.ac.warwick.sso.client.{ShireCommand, SSOConfiguration}
import uk.ac.warwick.sso.client.cache.UserCache
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.cache.Cache

/**
 * Yay, factory beans.
 *
 * Creates a ShireCommand.
 */
class ShireCommandProvider @Inject() (
    config: SSOConfiguration,
    userCache: UserCache,
    userIdCache: Cache[String, User]
  ) extends Provider[ShireCommand] {
  def get() = new ShireCommand(config, userCache, userIdCache)
}
