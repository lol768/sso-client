package warwick.sso

import java.util.Arrays._

import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.http.message.BasicHeader
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.{Cookie, Results}
import play.api.test.FakeRequest
import uk.ac.warwick.sso.client.{SSOConfiguration, SSOClientHandler}
import org.mockito.Mockito._
import org.mockito.Matchers._

import play.api.test.Helpers._
import uk.ac.warwick.sso.client.core.Response

class SsoClientImplSpec extends PlaySpec with MockitoSugar with Results {

  "Authenticated ActionBuilder" should {

    val handler = mock[SSOClientHandler]
    val response = new Response
    val client = new SsoClientImpl(handler, new SSOConfiguration(new PropertiesConfiguration()))
    val action = client.Lenient { request => Ok("Great") }
    val noRunAction = client.Lenient { request => fail("Shouldn't run this block") }

    when(handler.handle(any())).thenReturn(response)

    "convert cookies" in {
      val alan = Cookie(name="alan", value="sugar")
      response.addCookie(PlayHttpRequest.toCoreCookie(alan))

      val result = action.apply(FakeRequest())
      cookies(of=result).get("alan") must be (Some(alan))
    }

    "convert headers" in {
      response.setHeaders(asList(new BasicHeader("alan", "sugar")))
      val result = action.apply(FakeRequest())
      headers(of=result).get("alan").get must be ("sugar")
    }

    "send redirect" in {
      response.setContinueRequest(true)
      // should set continueRequest to false
      response.setRedirect("http://www.example.net/googles")
      val result = noRunAction.apply(FakeRequest())
      headers(of=result).get("Location").get must be ("http://www.example.net/googles")
    }

  }

}
