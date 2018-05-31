package warwick.sso

import java.util.Arrays._

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import javax.inject.Inject
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.http.message.BasicHeader
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.iteratee.streams.IterateeStreams
import play.api.libs.iteratee.{Enumerator, Input, Iteratee, Step}
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.ac.warwick.sso.client.core.Response
import uk.ac.warwick.sso.client.trusted.TrustedApplicationHandler
import uk.ac.warwick.sso.client.{SSOClientHandler, SSOConfiguration}
import uk.ac.warwick.userlookup
import uk.ac.warwick.userlookup.UserBuilder

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

object WebsocketTesting {
  class A(ctx: LoginContext, out: ActorRef) extends Actor { def receive = PartialFunction.empty }
  object A {
    def props(ctx: LoginContext)(out: ActorRef) = Props(classOf[A], ctx, out)
  }

  /** Example of using SSOClient in a websocket. This code isn't actually executed,
    * but it's nice to know that it compiles
    */
  class C @Inject()(client: SSOClient)(implicit system: ActorSystem, mat: Materializer) extends InjectedController {

    import scala.concurrent.ExecutionContext.Implicits.global

    def actor: WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] { req =>
      client.withUser(req) { loginContext =>
        Future.successful(Right(ActorFlow.actorRef(A.props(loginContext))))
      }
    }

    def iteratee: WebSocket = WebSocket.acceptOrResult[String, String] { req =>
      client.withUser(req) { loginContext =>
        val name = loginContext.user.flatMap(_.name.full).getOrElse("nobody")
        val iteratee = Iteratee.foreach[String](println)
        val enumerator = Enumerator(s"Hello, $name!")
        Future.successful(Right({
          val publisher = IterateeStreams.enumeratorToPublisher(enumerator)
          val (subscriber, _) = IterateeStreams.iterateeToSubscriber(iteratee)
          Flow.fromSinkAndSource(Sink.fromSubscriber(subscriber), Source.fromPublisher(publisher))
        }))
      }
    }

    /**
      * Like Enumeratee.onEOF, however enumeratee.onEOF always gets fed an EOF (by the enumerator if nothing else).
      */
    private def onEOF[E](enumerator: Enumerator[E], action: () => Unit): Enumerator[E] = new Enumerator[E] {
      def apply[A](i: Iteratee[E, A]) = enumerator(wrap(i))

      def wrap[A](i: Iteratee[E, A]): Iteratee[E, A] = new Iteratee[E, A] {
        def fold[B](folder: (Step[E, A]) => Future[B])(implicit ec: ExecutionContext) = i.fold {
          case Step.Cont(k) => folder(Step.Cont {
            case eof @ Input.EOF =>
              action()
              wrap(k(eof))
            case other => wrap(k(other))
          })
          case other => folder(other)
        }(ec)
      }
    }
  }
}

class SsoClientImplSpec extends PlaySpec with MockitoSugar with Results with GuiceOneAppPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val materializer: Materializer = app.materializer

  class Context {
    val ssoUser: userlookup.User = new userlookup.User("jim") {
      setFoundUser(true)
      setWarwickId("1234567")
    }
    val user = User(ssoUser)

    val handler = mock[SSOClientHandler]
    val trustedAppHandler = mock[TrustedApplicationHandler]
    val response = new Response
    val groupService = mock[GroupService]
    val roleService = mock[RoleService]
    val bodyParsers = PlayBodyParsers()
    val client: SSOClient = new SSOClientImpl(handler, trustedAppHandler, new SSOConfiguration(new PropertiesConfiguration()), groupService, roleService)
    val action: Action[ByteString] = client.Lenient(bodyParsers.byteString) { _: AuthenticatedRequest[_] => Ok("Great") }
    val noRunAction: Action[AnyContent] = client.Lenient(bodyParsers.default) { _: AuthenticatedRequest[_] => fail("Shouldn't run this block") }

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

    "reuse existing data from a request attribute" in new Context {
      val context = new LoginContextImpl(null, Option(user), Option(user))(groupService, roleService)
      val req = FakeRequest().addAttr(AuthenticatedRequest.LoginContextDataAttr, context)

      val newContext = client.withUser(req) { loginContext =>
        Future.successful(Right(loginContext))
      }.futureValue.right.get

      newContext.user.get must be (user)
      newContext.actualUser.get must be (user)

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
      headers(of=result).get("alan") must be (Some("sugar"))
    }

    "send redirect" in new Context {
      response.setContinueRequest(true)
      // should set continueRequest to false
      response.setRedirect("http://www.example.net/googles")
      val result = noRunAction.apply(FakeRequest())
      headers(of=result).get("Location") must be (Some("http://www.example.net/googles"))
    }

    "not send redirect" in new Context {
      response.setRedirect("http://www.example.net/googles")
      val result = client.Lenient(bodyParsers.default).disallowRedirect { _: AuthenticatedRequest[_] => Ok("super") }.apply(FakeRequest())
      headers(of=result).get("Location") mustBe None
    }

    "reuse existing data from a request attribute" in new Context {
      val context = new LoginContextImpl(null, Option(user), Option(user))(groupService, roleService)
      val req = FakeRequest().addAttr(AuthenticatedRequest.LoginContextDataAttr, context)

      val myAction: Action[AnyContent] = client.Lenient(bodyParsers.default) { implicit request =>
        val newContext = request.context
        newContext.user.get must be (user)
        newContext.actualUser.get must be (user)
        Ok
      }

      myAction.apply(req)
    }

  }

}
