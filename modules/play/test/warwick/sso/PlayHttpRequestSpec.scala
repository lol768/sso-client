package warwick.sso

import java.util.Arrays.asList
import java.util.Collections.emptyList
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.http.HeaderNames

import scala.collection.JavaConverters._

import org.scalatestplus.play.PlaySpec
import play.api.mvc.{Cookie, Headers, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest

class PlayHttpRequestSpec extends PlaySpec with GeneratorDrivenPropertyChecks {

  "PlayHttpRequestTest" should {

    val req = new PlayHttpRequest(
      FakeRequest(
        method = "POST",
        uri = "/cool/website?querykey=a&querykey=b&bodykey=overriden",
        headers = Headers(
          HeaderNames.HOST -> "example.org"
        ),
        body = AnyContentAsFormUrlEncoded(Map(
          "bodykey" -> Seq("1", "2")
        )),
        secure = true
      ).withCookies(Cookie(
        name = "x",
        value = "y"
      ))
    )

    "getParameter" in {
      req.getParameter("querykey") must be (asList("a","b"))
      req.getParameter("bodykey") must be (asList("1","2"))
      req.getParameter("nope") must be (emptyList)
    }

    "getParameterNames" in {
      req.getParameterNames must be (Set("querykey","bodykey").asJava)
    }

    "getAttribute" in {
      forAll { key: String =>
        req.getAttribute(key) must be (null)
      }
    }

    "getRequestURL" in {
      req.getRequestURL must be ("https://example.org/cool/website?querykey=a&querykey=b&bodykey=overriden")

      new PlayHttpRequest(FakeRequest("GET", "/").withHeaders("Host" -> "example.com"))
        .getRequestURL must be ("http://example.com/")
    }

    "getCookies" in {
      req.getCookies must have length(1)
      val cookie = req.getCookies.get(0)
      cookie.getName must be ("x")
      cookie.getValue must be ("y")
    }

    "getQueryString" in {
      req.getQueryString must be ("querykey=a&querykey=b&bodykey=overriden")
    }

    "getRequestURI" in {
      req.getRequestURI must be ("/cool/website")
    }

    "getHeaders" in {
      req.getHeaders(HeaderNames.HOST) must be (asList("example.org"))
      req.getHeaders("invalid") must be (emptyList)
    }

    "getHeader" in {
      req.getHeader(HeaderNames.HOST) must be ("example.org")
      req.getHeader("invalid") must be (null)
    }

    "getRemoteAddr" in {
      req.getRemoteAddr must be ("127.0.0.1")
    }

    "getMethod" in {
      for (method <- Seq("GET","POST","DELETE")) {
        new PlayHttpRequest(FakeRequest(method, "/")).getMethod must be (method)
      }
    }



  }
}
