package warwick.sso

import javax.inject.{Provider, Inject}

import org.slf4j.LoggerFactory
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import uk.ac.warwick.sso.client.{SSOException, AttributeAuthorityResponseFetcherImpl, ShireCommand, SSOConfiguration}
import uk.ac.warwick.sso.client.cache.UserCache
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.cache.Cache

import scala.util.{Failure, Success, Try}

object AssertionConsumer {
  case class SamlData(saml: String, target: String)

  val LOGGER = LoggerFactory.getLogger(classOf[AssertionConsumer])

  val SamlForm = Form(
    mapping(
      "SAMLResponse" -> text,
      "TARGET" -> text
    )(SamlData.apply)(SamlData.unapply)
  )
}

/**
 * Equivalent of ShireServlet. Receives SAML assertion posted
 * from Websignon after login, processes it, fetches attributes
 * from Websignon, and sets up the session.
 */
class AssertionConsumer @Inject() (
    config: SSOConfiguration,
    shireCommandProvider: Provider[ShireCommand]
  ) extends Controller {
  import AssertionConsumer._

  def get = Action {
    MethodNotAllowed
  }

  def post = Action { implicit request =>
    SamlForm.bindFromRequest.fold(
      withErrors => BadRequest(withErrors.errors.map(_.message).mkString(", ")),
      data => handle(request, data)
    )
  }

  def handle(request: Request[AnyContent], data: SamlData): Result = {
    val command = shireCommandProvider.get()
    command.setAaFetcher(new AttributeAuthorityResponseFetcherImpl(config))
    command.setRemoteHost(remoteHost(request))
    val cookie = Try(command.process(data.saml, data.target)).map(toPlayCookie) match {
      case Success(c) =>
        Option(c)
      case Failure(e) =>
        LOGGER.warn("Could not generate cookie", e)
        None
    }

    val redirect = TemporaryRedirect(data.target).withHeaders(
      "P3P" -> "CP=\"CAO PSA OUR\""
    )

    cookie match {
      case Some(c) =>
        LOGGER.debug("User being redirected to target with new SSC")
        redirect.withCookies(Seq(c) : _*)
      case None if hasExistingServiceCookie(request) =>
        LOGGER.debug("User being redirected to target but they didn't get a new SSC, so we are reusing the old one")
        redirect
      case None =>
        LOGGER.warn("No SSC cookie returned to client, nor do they have a previous SSC")
        // Delete the SSO-LTC cookie
        redirect.discardingCookies(DiscardingCookie(name = "SSO-LTC", domain = Some(".warwick.ac.uk")))
    }
  }

  /**
   *
   */
  def hasExistingServiceCookie(req: Request[AnyContent]) =
    req.cookies.get(config.getString("shire.sscookie.name")).isDefined

  def remoteHost(request: Request[AnyContent]): String = {
    val xff = request.headers.get("X-Forwarded-For")
    xff.getOrElse( request.remoteAddress )
  }

  def toPlayCookie(cookie: uk.ac.warwick.sso.client.core.Cookie) : Cookie =
    Cookie(
      name = cookie.getName,
      value = cookie.getValue,
      // -1 means "until browser close", convert that to None
      maxAge =
        if (cookie.isDelete) Some(0)
        else Option(cookie.getMaxAge).filter(_ >= 0),
      path = cookie.getPath,
      domain = Option(cookie.getDomain),
      secure = cookie.isSecure,
      httpOnly = cookie.isHttpOnly
    )

}

