package warwick.sso

import javax.inject.{Provider, Inject}

import play.api.mvc._
import uk.ac.warwick.sso.client.{SSOConfiguration, SSOClientHandler}
import uk.ac.warwick.sso.client.core.{LinkGenerator, Response}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

import scala.concurrent.Future

trait LoginContext {
  val user: User
  def loginUrl(target: Option[String]): String
}

class LoginContextImpl(linkGenerator: LinkGenerator, val user: User) extends LoginContext {
  def loginUrl(target: Option[String]) = {
    target.foreach(linkGenerator.setTarget)
    linkGenerator.getLoginUrl
  }
}

class AuthenticatedRequest[A, U](val context: U, request: Request[A]) extends WrappedRequest[A](request)

trait SsoClient {

  type AuthRequest[A] = AuthenticatedRequest[A, LoginContext]

  /**
   * Fetches any existing user session and provides it as an AuthenticatedRequest which has a User.
   */
  val Authenticated: ActionBuilder[AuthRequest]

  def linkGenerator(request: Request[_]): LinkGenerator
}


class SsoClientImpl @Inject() (
    handler: SSOClientHandler,
    configuration: SSOConfiguration
  ) extends SsoClient {

  import play.api.libs.concurrent.Execution.Implicits._

  def linkGenerator(request: Request[_]) = {
    val req = new PlayHttpRequest(request)
    new LinkGenerator(configuration, req)
  }

  lazy val Authenticated = new ActionBuilder[({type R[A] = AuthenticatedRequest[A, LoginContext]})#R] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A, LoginContext]) => Future[Result]): Future[Result] = {
      val req = new PlayHttpRequest(request)
      val response: Future[Response] = Future { handler.handle(req) }

      /** A bunch of munging to turn the sso-client-core Response into a Play result */
      response.flatMap { response =>
        val result = if (response.isContinueRequest) {
          val user = response.getUser
          val ctx = new LoginContextImpl(linkGenerator(request), user)
          block(new AuthenticatedRequest(ctx, request))
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
