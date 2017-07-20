package warwick.sso

import javax.inject.Inject

import play.api.mvc._
import uk.ac.warwick.sso.client.core.{LinkGenerator, LinkGeneratorImpl, Response}
import uk.ac.warwick.sso.client.trusted.TrustedApplicationHandler
import uk.ac.warwick.sso.client.{SSOClientHandler, SSOConfiguration}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

trait LoginContext {
  val user: Option[User]
  /**
    * When masquerading, this is the actual user who is masquerading, while `user`
    * will be the apparent user being masqueraded as.
    */
  val actualUser: Option[User]
  def loginUrl(target: Option[String]): String
  def isMasquerading: Boolean = user != actualUser

  def userHasRole(role: RoleName): Boolean
  def actualUserHasRole(role: RoleName): Boolean
}

class LoginContextImpl(linkGenerator: LinkGenerator, val user: Option[User], val actualUser: Option[User] = None)(implicit groupService: GroupService, roleService: RoleService) extends LoginContext {
  def loginUrl(target: Option[String]) = {
    target.foreach(linkGenerator.setTarget)
    linkGenerator.getLoginUrl
  }

  override def userHasRole(role: RoleName) = user.exists(hasRole(role))

  override def actualUserHasRole(role: RoleName) = actualUser.exists(hasRole(role))

  private def hasRole(role: RoleName)(user: User) =
    groupService.isUserInGroup(user.usercode, roleService.getRole(role).groupName).getOrElse(false)
}

class AuthenticatedRequest[A](val context: LoginContext, val request: Request[A]) extends WrappedRequest[A](request)

trait SSOClient {

  type AuthRequest[A] = AuthenticatedRequest[A]

  /**
   * Fetches any existing user session and provides it as an AuthenticatedRequest which has a User.
   */
  def Lenient(parser: BodyParser[AnyContent]): SSOActionBuilder

  /**
   * Like Lenient, but if no user was present it automatically redirects to login so that a user
   * is required.
   */
  def Strict(parser: BodyParser[AnyContent]): ActionBuilder[AuthRequest, AnyContent]

  def RequireRole(role: RoleName, otherwise: AuthRequest[_] => Result)(parser: BodyParser[AnyContent]): ActionBuilder[AuthRequest, AnyContent]

  def RequireActualUserRole(role: RoleName, otherwise: AuthRequest[_] => Result)(parser: BodyParser[AnyContent]): ActionBuilder[AuthRequest, AnyContent]

  /** The type of block you can pass to withUser. */
  type TryAcceptResult[A] = Future[Either[Result, A]]

  /**
   * This is for use with WebSockets, which don't use Actions. A Controller method might look like this:
   *
   *     def userMessages = WebSocket.tryAcceptWithActor[JsValue, JsValue] { req =>
   *       client.withUser(req) { loginContext =>
   *         Future.successful(Right(A.props(loginContext) _))
   *       }
   *     }
   *
   * Where `object A { def props(ctx: LoginContext)(out: ActorRef): Props }` is defined. The first param
   * list is for yu to pass any dependencies to the actor, and the second is kept to be called by the
   * WebSocket class as it requires an `ActorRef => Props` function.
   *
   * Obviously you can do proper Future stuff if you need to, and you can return a Left[Result] if you
   * want to immediately return an HTTP response, though most clients disconnect if you do this.
   */
  def withUser[A](request: RequestHeader)(block: LoginContext => TryAcceptResult[A]): TryAcceptResult[A]

  def linkGenerator(request: RequestHeader): LinkGenerator

  abstract class SSOActionBuilder(val parser: BodyParser[AnyContent])(implicit ec: ExecutionContext) extends ActionBuilder[AuthRequest, AnyContent] {
    override protected val executionContext = ec
    def disallowRedirect: SSOActionBuilder
  }
}

class SSOClientImpl @Inject()(
  handler: SSOClientHandler,
  trustedAppsHandler: TrustedApplicationHandler,
  configuration: SSOConfiguration,
  implicit val groupService: GroupService,
  implicit val roleService: RoleService
)(implicit ec: ExecutionContext) extends SSOClient {

  import play.api.mvc.Results._

  def linkGenerator(request: RequestHeader) = {
    val req = new PlayHttpRequestHeader(request)
    new LinkGeneratorImpl(configuration, req)
  }

  def Strict(parser: BodyParser[AnyContent]) = Lenient(parser) andThen requireCondition(_.context.user.nonEmpty, otherwise = redirectToSSO)
  def Lenient(parser: BodyParser[AnyContent]) = FindUser(bodyParser = parser)

  override def RequireRole(role: RoleName, otherwise: AuthRequest[_] => Result)(parser: BodyParser[AnyContent]) = Strict(parser) andThen requireCondition(_.context.userHasRole(role), otherwise)

  override def RequireActualUserRole(role: RoleName, otherwise: AuthRequest[_] => Result)(parser: BodyParser[AnyContent]) = Strict(parser) andThen requireCondition(_.context.actualUserHasRole(role), otherwise)

  implicit class BonusResponse(response: Response) {
    lazy val user: Option[User] = Option(response.getUser).filter(User.hasUsercode).map(User.apply)

    lazy val actualUser: Option[User] = Option(response.getActualUser).filter(User.hasUsercode).map(User.apply)

    def isError: Boolean = response.getStatusCode.toString.startsWith("5")

    def hasUser: Boolean = user.isDefined
  }

  /**
   * Action which takes a regular Request and converts it into an AuthRequest,
   * containing an Option[User].
   *
   * It may sometimes generate a redirect, such as when it finds a global session
   * cookie and it thinks the user should be redirect through websignon to set up
   * a local session. But it won't require a logged in user - if there is no session
   * the user will simply be None.
   */
  case class FindUser(permitRedirect: Boolean = true, bodyParser: BodyParser[AnyContent]) extends SSOActionBuilder(bodyParser) {

    def disallowRedirect = copy(permitRedirect = false)

    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      val req = new PlayHttpRequest(request)

      /** A bunch of munging to turn the sso-client-core Response into a Play result */
      val handlerResponses = for {
        ssoResponse <- Future(handler.handle(req))
        trustedAppsResponse <- Future(trustedAppsHandler.handle(req))
      } yield (ssoResponse, trustedAppsResponse)

      handlerResponses.flatMap { case (ssoResponse, trustedAppsResponse) =>
        // Use the trusted apps response if it produced a user or an error
        val response = Option(trustedAppsResponse).filter(r => r.hasUser || r.isError).getOrElse(ssoResponse)

        val context = new LoginContextImpl(linkGenerator(request), response.user, response.actualUser)

        val result = if (response.isContinueRequest || !permitRedirect) {
          block(new AuthenticatedRequest(context, request))
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

  override def withUser[A](req: RequestHeader)(block: (LoginContext) => TryAcceptResult[A]): TryAcceptResult[A] = {
    val request = new PlayHttpRequestHeader(req)
    val response = Future{ handler.handle(request) }
    response.flatMap { response =>
      val user = Option(response.getUser).filter(User.hasUsercode)
      val actualUser = Option(response.getActualUser).filter(User.hasUsercode)
      val ctx = new LoginContextImpl(linkGenerator(req), user.map(User.apply), actualUser.map(User.apply))
      block(ctx)
    }
  }

  class RequireConditionActionFilter(block: AuthRequest[_] => Boolean, otherwise: AuthRequest[_] => Result)(implicit val executionContext: ExecutionContext) extends ActionFilter[AuthRequest] {
    override protected def filter[A](request: AuthRequest[A]) =
      Future.successful {
        if (block(request)) None
        else Some(otherwise(request))
      }
  }

  private def requireCondition(block: AuthRequest[_] => Boolean, otherwise: AuthRequest[_] => Result) =
    new RequireConditionActionFilter(block, otherwise)

  private def redirectToSSO(request: AuthRequest[_]) = Redirect(request.context.loginUrl(None))

}
