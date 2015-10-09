package warwick.sso

import scala.util.Try

object TryTap {
  implicit class Implicit(t: Try[_]) extends AnyVal {
    def tap(f : (Throwable)=>Unit) = t.recover {
      case e => f(e); e
    }
  }
}
