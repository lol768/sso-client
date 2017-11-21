package warwick.sso

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.Future
import scala.util.Success
import Users._
import akka.stream.Materializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.JsValue

class BasicAuthTest extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite {
  import play.api.mvc.Results._

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val materializer: Materializer = app.materializer

  trait Context {

    val userlookup = mock[UserLookupService]
    val sso = mock[SSOClient]
    val groupService = mock[GroupService]
    val roleService = mock[RoleService]
    val bodyParsers = PlayBodyParsers()
    val auth = new BasicAuthImpl(userlookup, sso, groupService, roleService)

    val deniedResult = Forbidden("Custom denied message")
    def deniedAction(request: RequestHeader) = Future.successful(deniedResult)

    // How you might get your app-specific ActionBuilder
    // with your choice of perm-denied result.
    def Secured = auth.Check(deniedAction)(bodyParsers.default)

    val action = Secured(bodyParsers.default) { request: AuthenticatedRequest[AnyContent] =>
      val who = request.context.user.map(_.usercode.string).getOrElse("anon")
      Ok(s"Hello, $who.")
    }
  }

  "Basic auth" should {
    "reject a missing header" in new Context {
      action(FakeRequest()).futureValue must be (deniedResult)
    }

    "reject a nonsense header" in new Context {
      val auths = Seq("Basic bananas", "Basic aGVyb25maXNoCg==")
      for (auth <- auths) {
        action(FakeRequest().withHeaders(
          "Authorization" -> auth
        )).futureValue must be (deniedResult)
      }
    }

    "allow a correct header" in new Context {
      when(userlookup.basicAuth("heron", "fish")) thenReturn Success(Some(alana))

      val result = action(FakeRequest().withHeaders(
        "Authorization" -> "Basic aGVyb246ZmlzaAo="
      ))

      contentAsString(result) must be ("Hello, alana.")
      status(result) must be (200)
    }

  }

}
