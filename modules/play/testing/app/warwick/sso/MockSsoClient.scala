package warwick.sso

import javax.inject.Inject

import play.api.mvc.{ActionBuilder, Request, RequestHeader, Result}

import scala.concurrent.Future

class MockSsoClient @Inject() (
    loginContext: LoginContext
  ) extends SsoClient {

  object Wrap extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthRequest[A]) => Future[Result]): Future[Result] =
      block(new AuthRequest(loginContext, request))
  }

  override val Lenient = Wrap
  override val Strict = Wrap

  override def withUser[A](request: RequestHeader)(block: (LoginContext) => TryAcceptResult[A]): TryAcceptResult[A] =
    block(loginContext)
}
