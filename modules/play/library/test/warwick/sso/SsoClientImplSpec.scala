package warwick.sso

import java.util.Arrays._

import akka.actor.{Actor, ActorRef, Props}
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.http.message.BasicHeader
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.JsValue
import play.api.mvc.{Controller, Cookie, Results, WebSocket}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.ac.warwick.sso.client.core.Response
import uk.ac.warwick.sso.client.trusted.TrustedApplicationHandler
import uk.ac.warwick.sso.client.{SSOClientHandler, SSOConfiguration}

import scala.concurrent.Future

object WebsocketTesting {
  class A(ctx: LoginContext, out: ActorRef) extends Actor { def receive = PartialFunction.empty }
  object A {
    def props(ctx: LoginContext)(out: ActorRef) = Props(classOf[A], ctx, out)
  }

  /** Example of using SSOClient in a websocket. This code isn't actually executed,
    * but it's nice to know that it compiles
    */
  class C(client: SSOClient) extends Controller {
    import play.api.Play.current

    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val mat = current.materializer

    def actor = WebSocket.tryAcceptWithActor[JsValue, JsValue] { req =>
      client.withUser(req) { loginContext =>
        Future.successful(Right(A.props(loginContext) _))
      }
    }
    def iteratee = WebSocket.tryAccept[String] { req =>
      client.withUser(req) { loginContext =>
        val name = loginContext.user.flatMap(_.name.full).getOrElse("nobody")
        val in = Iteratee.foreach[String](println)
        val out = Enumerator(s"Hello, ${name}!")
        Future.successful(Right((in, out)))
      }
    }
  }
}

class SsoClientImplSpec extends PlaySpec with MockitoSugar with Results {

  class Context {
    val handler = mock[SSOClientHandler]
    val trustedAppHandler = mock[TrustedApplicationHandler]
    val response = new Response
    val groupService = mock[GroupService]
    val roleService = mock[RoleService]
    val client: SSOClient = new SSOClientImpl(handler, trustedAppHandler, new SSOConfiguration(new PropertiesConfiguration()), groupService, roleService)
    val action = client.Lenient { request => Ok("Great") }
    val noRunAction = client.Lenient { request => fail("Shouldn't run this block") }

    when(handler.handle(any())).thenReturn(response)
  }
  
  "withUser" should {
    "provide a user for use in a websocket" in new Context {
      import WebsocketTesting._

      val req = FakeRequest()
      val testActor: ActorRef = ActorRef.noSender

      val result = client.withUser(req) { loginContext =>
        Future.successful(Right(A.props(loginContext) _))
      }

      // get result and pass in ActorRef as the WebSocket code would do.
      val props = result.futureValue.right.get.apply(testActor)
      props.args(0) must be (a[LoginContext])
      props.args(1) must be (testActor)

    }
  }

  "Authenticated ActionBuilder" should {

    "convert cookies" in new Context {
      val alan = Cookie(name="alan", value="sugar")
      response.addCookie(PlayHttpRequest.toCoreCookie(alan))

      val result = action.apply(FakeRequest())
      cookies(of=result).get("alan") must be (Some(alan))
    }

    "convert headers" in new Context {
      response.setHeaders(asList(new BasicHeader("alan", "sugar")))
      val result = action.apply(FakeRequest())
      headers(of=result).get("alan").get must be ("sugar")
    }

    "send redirect" in new Context {
      response.setContinueRequest(true)
      // should set continueRequest to false
      response.setRedirect("http://www.example.net/googles")
      val result = noRunAction.apply(FakeRequest())
      headers(of=result).get("Location").get must be ("http://www.example.net/googles")
    }

    "not send redirect" in new Context {
      response.setRedirect("http://www.example.net/googles")
      val result = client.Lenient.disallowRedirect(request => Ok("super")).apply(FakeRequest())
      headers(of=result).get("Location") mustBe None
    }

  }

}
