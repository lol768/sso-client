package warwick.sso

import javax.inject.Inject

import play.api.mvc.{Results, Result, Request, ActionBuilder}
import play.api.mvc.Security.AuthenticatedRequest
import uk.ac.warwick.sso.client.SSOClientHandler
import uk.ac.warwick.sso.client.core.Response
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

import scala.concurrent.Future

trait SsoClient {
  type AuthRequest[A] = AuthenticatedRequest[A, User]

  /**
   * Fetches any existing user session and provides it as an AuthenticatedRequest which has a User.
   */
  val Authenticated: ActionBuilder[AuthRequest]
}


class SsoClientImpl @Inject() (
    handler: SSOClientHandler
  ) extends SsoClient {

  import play.api.libs.concurrent.Execution.Implicits._

  lazy val Authenticated = new ActionBuilder[({type R[A] = AuthenticatedRequest[A, User]})#R] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A, User]) => Future[Result]): Future[Result] = {
      val req = new PlayHttpRequest(request)
      val response: Future[Response] = Future { handler.handle(req) }

      /** A bunch of munging to turn the sso-client-core Response into a Play result */
      response.flatMap { response =>
        val result = if (response.isContinueRequest) {
          val user = response.getUser
          block(new AuthenticatedRequest(user, request))
        } else {
          // Handler wants to do a redirect or something
          Future.successful(Results.Redirect(response.getRedirect))
        }

        // TODO deal with the response outputStream? only used for oauth.

        result.map { result =>
          result
            .withCookies(response.getCookies.asScala.map(PlayHttpRequest.toPlayCookie) : _*)
            .withHeaders(response.getHeaders.asScala.map(h => h.getName -> h.getValue) : _*)
        }
      }
    }
  }

}
