package warwick.sso

import com.google.inject.Inject
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import uk.ac.warwick.sso.client.SSOClientHandlerImpl
import uk.ac.warwick.sso.client.SSOClientHandlerImpl.DEFAULT_MASQUERADE_COOKIE_NAME
import uk.ac.warwick.userlookup.UserLookupInterface

object MasqueradeController {
  val ErrorFlashKey = "MasqueradeError"
  val UsercodeFormKey = "usercode"

  val MasqueradeNotEnabled = "This application does not support masquerading as other users"
}

class MasqueradeController @Inject()(
                                      ssoClient: SSOClient,
                                      configuration: Configuration,
                                      userLookupService: UserLookupService,
                                      userLookup: UserLookupInterface
                                    ) extends Controller {

  import MasqueradeController._

  private val UsercodeForm = Form(
    single(UsercodeFormKey -> nonEmptyText)
      .transform(s => Usercode(s), (u: Usercode) => u.string)
  )

  val masqueradeGroupName = configuration.getString("sso-client.masquerade.group")

  val masqueradeCookieName = configuration.getString("sso-client.masquerade.cookie.name")
    .getOrElse(DEFAULT_MASQUERADE_COOKIE_NAME)
  val masqueradeCookiePath = configuration.getString("sso-client.masquerade.cookie.path")
    .orElse(configuration.getString("sso-client.shire.sscookie.path"))
    .getOrElse("/")
  val masqueradeCookieDomain = configuration.getString("sso-client.masquerade.cookie.domain")
    .orElse(configuration.getString("sso-client.shire.sscookie.domain"))

  def masquerade = ssoClient.Strict { implicit request =>
    request.context.actualUser.map { actualUser =>
      masqueradeGroupName.map { groupName =>
        UsercodeForm.bindFromRequest().fold(
          formWithErrors => error("You must provide a usercode"),
          usercode => {
            if (!userLookup.getGroupService.isUserInGroup(actualUser.usercode.string, groupName)) {
              error("You do not have permission to masquerade")
            } else if (actualUser.usercode == usercode) {
              error("Cannot masquerade as yourself")
            } else if (!userExists(usercode)) {
              error(s"Usercode '${usercode.string}' does not exist")
            } else {
              redirectBack().withCookies(masqueradeAs(usercode))
            }
          }
        )
      }.getOrElse(NotFound(MasqueradeNotEnabled))
    }.getOrElse(throw new IllegalStateException("actualUser not defined within SSOClient Strict action"))
  }

  def unmask = Action { implicit request =>
    masqueradeGroupName.map(_ => redirectBack().discardingCookies(discardMasqueradeAs()))
      .getOrElse(NotFound(MasqueradeNotEnabled))
  }

  private def masqueradeAs(usercode: Usercode): Cookie =
    Cookie(
      name = masqueradeCookieName,
      value = usercode.string,
      domain = masqueradeCookieDomain,
      path = masqueradeCookiePath
    )

  private def discardMasqueradeAs(): DiscardingCookie =
    DiscardingCookie(
      name = masqueradeCookieName,
      domain = masqueradeCookieDomain,
      path = masqueradeCookiePath
    )

  private def redirectBack()(implicit request: Request[_]): Result =
    Redirect(request.headers.get(REFERER).getOrElse("/"))

  private def error(message: String)(implicit request: Request[_]): Result =
    redirectBack().flashing(ErrorFlashKey -> message)

  private def userExists(usercode: Usercode): Boolean =
    userLookupService.getUsers(Seq(usercode)).map(_.nonEmpty).getOrElse(false)

}
