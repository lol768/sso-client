package warwick.sso

import javax.inject.Inject

import org.slf4j.LoggerFactory
import play.api.mvc.InjectedController
import uk.ac.warwick.sso.client.SSOToken
import uk.ac.warwick.sso.client.cache.UserCache
import uk.ac.warwick.util.core.StringUtils


/**
 * Handles back-channel requests from Websignon, for implementing single-logout.
 */
class LogoutController @Inject() (userCache: UserCache) extends InjectedController {

  private val LOGGER = LoggerFactory.getLogger(getClass)

  def post = Action { request =>
    val ssoToken = request.getQueryString("logoutTicket")
      .filter(StringUtils.hasText)
      .map { ticket => new SSOToken(ticket.trim, SSOToken.SSC_TICKET_TYPE) }

    val success = ssoToken.exists { token =>
      if (userCache.get(token) != null) {
        userCache.remove(token)
        LOGGER.info(s"Logout attempt succeeded as ssc (${token}) was found in cache")
        true
      } else {
        LOGGER.info(s"Logout attempt failed because the ssc (${token}) was not found in the user cache")
        false
      }
    }

    Ok(success.toString)
  }

}
