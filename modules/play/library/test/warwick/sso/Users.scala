package warwick.sso

import uk.ac.warwick.userlookup

object Users {

  val alana = fakeUser("alana")
  val betty = fakeUser("betty")
  val clara = fakeUser("clara")

  private def fakeUser(usercode: String): User = {
    val u = new userlookup.User(usercode)
    u.setFoundUser(true)
    User(u)
  }

}
