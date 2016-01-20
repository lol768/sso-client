package warwick.sso

import javax.inject.{Provider, Inject}

import play.api.mvc._
import uk.ac.warwick.sso.client.{SSOConfiguration, SSOClientHandler}
import uk.ac.warwick.sso.client.core.{Response, LinkGenerator, LinkGeneratorImpl}
import scala.collection.JavaConverters._

import scala.concurrent.Future

trait LoginContext {
  val user: Option[User]
  /**
    * When masquerading, this is the actual user who is masquerading, while `user`
    * will be the apparent user being masqueraded as.
    */
  val actualUser: Option[User]
  def loginUrl(target: Option[String]): String
  def isMasquerading: Boolean = user.contains(actualUser)
}

class LoginContextImpl(linkGenerator: LinkGenerator, val user: Option[User], val actualUser: Option[User] = None) extends LoginContext {
  def loginUrl(target: Option[String]) = {
    target.foreach(linkGenerator.setTarget)
    linkGenerator.getLoginUrl
  }
}

class AuthenticatedRequest[A](val context: LoginContext, val request: Request[A]) extends WrappedRequest[A](request)

trait SSOClient {

  type AuthRequest[A] = AuthenticatedRequest[A]

  /**
   * Fetches any existing user session and provides it as an AuthenticatedRequest which has a User.
   */
  val Lenient: ActionBuilder[AuthRequest]

  /**
   * Like Lenient, but if no user was present it automatically redirects to login so that a user
   * is required.
   */
  val Strict: ActionBuilder[AuthRequest]

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
}


class SSOClientImpl @Inject()(
    handler: SSOClientHandler,
    configuration: SSOConfiguration
  ) extends SSOClient {

  import play.api.libs.concurrent.Execution.Implicits._
  import play.api.mvc.Results._

  def linkGenerator(request: RequestHeader) = {
    val req = new PlayHttpRequestHeader(request)
    new LinkGeneratorImpl(configuration, req)
  }

  lazy val Strict = Lenient andThen RequireLogin

  lazy val Lenient = FindUser

  object RequireLogin extends ActionFilter[AuthRequest] {
    override protected def filter[A](request: AuthRequest[A]): Future[Option[Result]] =
      request.context.user match {
        case None => Future.successful(Some(Redirect(request.context.loginUrl(None))))
        case Some(_) => Future.successful(None)
      }
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
  object FindUser extends ActionBuilder[AuthRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      val req = new PlayHttpRequest(request)
      val response: Future[Response] = Future { handler.handle(req) }

      /** A bunch of munging to turn the sso-client-core Response into a Play result */
      response.flatMap { response =>
        val result = if (response.isContinueRequest) {
          val user = Option(response.getUser).filter(User.hasUsercode)
          val actualUser = Option(response.getActualUser).filter(User.hasUsercode)
          val ctx = new LoginContextImpl(linkGenerator(request), user.map(User.apply), actualUser.map(User.apply))
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

  override def withUser[A](req: RequestHeader)(block: (LoginContext) => TryAcceptResult[A])
      : TryAcceptResult[A] = {

    val request = new PlayHttpRequestHeader(req)
    val response = Future{ handler.handle(request) }
    response.flatMap { response =>
      val user = Option(response.getUser).filter(User.hasUsercode)
      val actualUser = Option(response.getActualUser).filter(User.hasUsercode)
      val ctx = new LoginContextImpl(linkGenerator(req), user.map(User.apply), actualUser.map(User.apply))
      block(ctx)
    }

  }
}
