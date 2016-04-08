package warwick.sso

import java.util.Base64
import javax.inject.Inject

import play.api.mvc.{RequestHeader, Request, Result, ActionBuilder}

import scala.concurrent.Future
import scala.util._

/**
  * A service that provides a Basic Auth header check as an ActionBuilder.
  *
  * You will either get an AuthenticatedRequest containing a LoginContext,
  * or you'll receive the result as calculated from `denied`.
  */
trait BasicAuth {
  def Check(denied: RequestHeader => Future[Result]): ActionBuilder[AuthenticatedRequest]
}

/**
  * This is all the generic HTTP header parsing for Basic auth, so
  * you can re-use this in another implementation that might not check
  * credentials through UserLookup.
  */
object BasicAuth {
  val BasicAuthFormat = "Basic (.+)".r

  /** Extractor for the base64-encoded username:password format */
  object EncodedUserPass {
    def unapply(base64: String): Option[(String, String)] = Try(Base64.getDecoder.decode(base64))
      .map(arr => new String(arr, "UTF-8"))
      .map { str =>
        val (user :: pass :: _) = str.split(':').toList
        (user.trim, pass.trim)
      }
      .toOption
  }

  /** Extractor for a "Basic aGVyb25maXNoCg==" header value. Extracts the
    * username and password from the base64-encoded value. */
  object BasicAuthHeader {
    def unapply(headerValue: String): Option[(String, String)] = headerValue match {
      case BasicAuthFormat(EncodedUserPass(usercode, password)) => Some((usercode, password))
      case _ => None
    }
  }

}

class BasicAuthImpl @Inject()(userLookup: UserLookupService, sso: SSOClient, implicit val groupService: GroupService, implicit val roleService: RoleService) extends BasicAuth {
  import BasicAuth._

  override def Check(denied: RequestHeader => Future[Result]) : ActionBuilder[AuthenticatedRequest] = new ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]) = {
      request.headers.get("Authorization").flatMap {
        case BasicAuthHeader(usercode, pass) =>
          userLookup.basicAuth(usercode, pass).toOption.flatten.map { user =>
            block(new AuthenticatedRequest[A](ctx(request, user), request))
          }
        case _ => None
      } getOrElse {
        denied(request)
      }
    }
  }

  private def ctx(req: RequestHeader, u: User): LoginContext =
    new LoginContextImpl(sso.linkGenerator(req), Some(u), None)
}