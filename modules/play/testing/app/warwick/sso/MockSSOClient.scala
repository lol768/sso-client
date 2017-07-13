package warwick.sso

import javax.inject.Inject

import play.api.mvc._
import uk.ac.warwick.sso.client.core.LinkGenerator

import scala.concurrent.{ExecutionContext, Future}

class MockSSOClient @Inject()(
  loginContext: LoginContext,
  bodyParsers: PlayBodyParsers
)(implicit ec: ExecutionContext) extends SSOClient {

  object Wrap extends SSOActionBuilder(bodyParsers.default) {
    override def disallowRedirect = this
    override def invokeBlock[A](request: Request[A], block: (AuthRequest[A]) => Future[Result]): Future[Result] =
      block(new AuthRequest(loginContext, request))
  }

  override val Lenient = Wrap
  override val Strict = Wrap

  override def RequireRole(role: RoleName, otherwise: (AuthRequest[_]) => Result) = Wrap
  override def RequireActualUserRole(role: RoleName, otherwise: (AuthRequest[_]) => Result) = Wrap

  override def withUser[A](request: RequestHeader)(block: (LoginContext) => TryAcceptResult[A]): TryAcceptResult[A] =
    block(loginContext)

  override def linkGenerator(request: RequestHeader) = new LinkGenerator {
    private var target: Option[String] = None

    private def url(junk: String) = "https://signon.example.com/" + junk
    override def getLoginUrl: String = url("login")
    override def getLogoutUrl: String = url("logout")
    override def getNotLoggedInLink: String = url("login?notloggedin")
    override def getPermissionDeniedLink(loggedIn: Boolean): String = url("login?permdenied")
    override def setTarget(s: String): Unit = target = Option(s)
    override def getTarget: String = target.getOrElse(request.uri)
  }
}
