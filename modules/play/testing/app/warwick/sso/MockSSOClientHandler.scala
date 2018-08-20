package warwick.sso

import org.apache.commons.configuration.Configuration
import uk.ac.warwick.sso.client.cache.UserCache
import uk.ac.warwick.sso.client.core.{HttpRequest, OnCampusService, Response}
import uk.ac.warwick.sso.client.{AttributeAuthorityResponseFetcher, SSOClientHandler}
import uk.ac.warwick.userlookup.UserLookupInterface

import scala.beans.{BeanProperty, BooleanBeanProperty}

/**
  * Implementation of SSOClientHandler that never returns a user and never redirects.
  *
  * You can use this in tests by overiding the default and keeping the real SSOClient.
  * To set a user for a request, set the [[AuthenticatedRequest.LoginContextDataAttr]]
  * attribute to a [[LoginContextData]] containing your desired [[User]].
  */
class MockSSOClientHandler extends SSOClientHandler {
  // Always an empty response - we populate a request attribute to get users
  override def handle(request: HttpRequest): Response = {
    val r = new Response
    r.setContinueRequest(true)
    r
  }

  @BeanProperty
  var aaFetcher: AttributeAuthorityResponseFetcher = _

  @BeanProperty
  var cache: UserCache = _

  @BeanProperty
  var config: Configuration = _

  @BooleanBeanProperty
  var detectAnonymousOnCampusUsers: Boolean = false

  @BooleanBeanProperty
  var redirectToRefreshSession: Boolean = true

  @BeanProperty
  var userLookup: UserLookupInterface = _

  @BeanProperty
  var onCampusService: OnCampusService = _
}

