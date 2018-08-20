package warwick.sso

import javax.inject.Inject
import play.api.libs.typedmap.TypedKey
import play.api.mvc._
import uk.ac.warwick.sso.client.core.LinkGenerator

import scala.concurrent.{ExecutionContext, Future}

object MockSSOClient {
  val LoginContextAttr: TypedKey[LoginContext] = TypedKey.apply[LoginContext]("loginContext")
}

/**
  * Do-nothing implementation of SSOClient. Note that it doesn't enforce ANY rules, even Strict,
  * so you might find actions that require a user being executed even if no user is present.
  */
class MockSSOClient @Inject()(
  defaultLoginContext: LoginContext
)(implicit ec: ExecutionContext) extends SSOClient {

  private def currentContext(request: RequestHeader): LoginContext =
    request.attrs.get(MockSSOClient.LoginContextAttr).getOrElse(defaultLoginContext)

  case class Wrap[C](bodyParser: BodyParser[C]) extends SSOActionBuilder(bodyParser) {
    override def disallowRedirect: SSOActionBuilder[C] = this
    override def invokeBlock[A](request: Request[A], block: AuthRequest[A] => Future[Result]): Future[Result] =
      block(new AuthRequest(currentContext(request), request))
  }

  override def Lenient[C](parser: BodyParser[C]): SSOActionBuilder[C] =
    Wrap(parser)

  override def Strict[C](parser: BodyParser[C]): ActionBuilder[AuthRequest, C] =
    Wrap(parser)

  override def RequireRole[C](role: RoleName, otherwise: (AuthRequest[_]) => Result)(parser: BodyParser[C]): ActionBuilder[AuthRequest, C] =
    Wrap(parser)

  override def RequireActualUserRole[C](role: RoleName, otherwise: (AuthRequest[_]) => Result)(parser: BodyParser[C]): ActionBuilder[AuthRequest, C] =
    Wrap(parser)

  override def withUser[A](request: RequestHeader)(block: (LoginContext) => TryAcceptResult[A]): TryAcceptResult[A] =
    block(currentContext(request))

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
