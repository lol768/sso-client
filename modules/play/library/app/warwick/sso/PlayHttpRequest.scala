package warwick.sso

import java.util

import play.api.libs.typedmap.TypedKey
import play.api.mvc._
import uk.ac.warwick.sso.client.core.{Cookie, HttpRequest}

import scala.collection.JavaConverters._

/**
 * Extension of PlayHttpRequestHeader that takes a full request, and so
 * has access to POST parameters.
 */
class PlayHttpRequest[A](req: Request[A]) extends PlayHttpRequestHeader(req) {
  override def bodyParams = req.body match {
    case AnyContentAsFormUrlEncoded(data) => data
    case AnyContentAsMultipartFormData(data) => data.asFormUrlEncoded
    case b : MultipartFormData[_] => b.asFormUrlEncoded
    case _ => Map.empty
  }
}

/**
 * Implements the HttpRequest interface from sso-client-core, wrapping
 * Play's native request object so that we can pass it into the core
 * handler code.
 *
 * This version only takes headers, and so won't return any POST parameters,
 * since they're in the body.
 */
class PlayHttpRequestHeader(req: RequestHeader) extends HttpRequest {
  import PlayHttpRequest._

  protected def bodyParams: Map[String, Seq[String]] = Map.empty

  override def getParameter(s: String): util.List[String] =
    bodyParams.get(s)
      .orElse(req.queryString.get(s))
      .getOrElse(Nil)
      .asJava

  override def getParameterNames: util.Set[String] = (req.queryString.keySet ++ bodyParams.keySet).asJava

  override def getAttribute(s: String): AnyRef = req.attrs.get(TypedKey(s)).orNull

  override def getRequestURL: String = {
    val sb = new StringBuilder
    sb.append(if (req.secure) "https://" else "http://")
    sb.append(req.host)
    sb.append(req.path)
    sb.toString
  }

  override def getCookies: util.List[Cookie] = {
    try {
      req.cookies.map(toCoreCookie).toSeq
    } catch {
      case _=> Nil
    }
  }.asJava

  override def getQueryString: String = req.rawQueryString

  override def getQueryParameter(name: String): util.List[String] = req.queryString.getOrElse(name, Nil).asJava

  // Just the path, no query strings
  override def getRequestURI: String = req.path

  override def getHeaders(s: String): util.List[String] = req.headers.getAll(s).asJava

  override def getRemoteAddr: String = req.remoteAddress

  override def getMethod: String = req.method

  override def getHeader(s: String): String = req.headers.get(s).orNull


}

object PlayHttpRequest {

  def toPlayCookie(cookie: Cookie) : play.api.mvc.Cookie =
    play.api.mvc.Cookie(
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

  def toCoreCookie(cookie: play.api.mvc.Cookie) : Cookie = {
    val c = new Cookie(cookie.name, cookie.value)
    c.setDomain(cookie.domain.orNull)
    c.setHttpOnly(cookie.httpOnly)
    c.setSecure(cookie.secure)
    c.setPath(cookie.path)
    cookie.maxAge match {
      case None => c.setMaxAge(-1) // until browser close
      case Some(x) if x < 0 => c.setDelete(true)
      case Some(other) => c.setMaxAge(other)
    }
    c
  }
}