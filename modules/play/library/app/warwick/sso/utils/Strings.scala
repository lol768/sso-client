package warwick.sso.utils

/**
 * Created by nick on 15/10/15.
 */
object Strings {

  def notEmptyOption(input: String): Option[String] = Option(input).filterNot(_.isEmpty)

}
