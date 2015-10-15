package warwick.sso

import java.util

import play.api.mvc._
import uk.ac.warwick.sso.client.core.{Cookie, HttpRequest}
import scala.collection.JavaConverters._

/**
 * Implements the HttpRequest interface from sso-client-core, wrapping
 * Play's native request object so that we can pass it into the core
 * handler code.
 */
class PlayHttpRequest(req: Request[_]) extends HttpRequest {
  import PlayHttpRequest._

  private def bodyParams: Map[String, Seq[String]] = req.body match {
    case AnyContentAsFormUrlEncoded(data) => data
    case AnyContentAsMultipartFormData(data) => data.asFormUrlEncoded
    case b : MultipartFormData[_] => b.asFormUrlEncoded
    case _ => Map.empty
  }

  override def getParameter(s: String): util.List[String] =
    bodyParams.get(s)
      .orElse(req.queryString.get(s))
      .getOrElse(Nil)
      .asJava

  override def getParameterNames: util.Set[String] = (req.queryString.keySet ++ bodyParams.keySet).asJava

  // Attributes aren't a thing in Play requests
  override def getAttribute(s: String): AnyRef = null

  override def getRequestURL: String = {
    val sb = new StringBuilder
    sb.append(if (req.secure) "https://" else "http://")
    sb.append(req.host)
    sb.append(req.path)
    sb.toString
  }

  override def getCookies: util.List[Cookie] = req.cookies.map(toCoreCookie).toSeq.asJava

  override def getQueryString: String = req.rawQueryString

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