import controllers.FormKeys.{emailMaxLengthKey, emailaddressNotValidKey, emailaddressRequiredKey, loginPasswordRequiredKey}
import play.api.data.Forms

package object controllers {

  object FormKeys {
    val emailaddressRequiredKey = "emailaddress.error.required.field"
    val emailaddressNotValidKey = "emailaddress.error.not.valid.field"
    val emailMaxLengthKey = "emailaddress.error.maxLength.field"
    val loginPasswordRequiredKey = "loginpassword.error.required.field"
  }

  val emailValidator = {
    Forms.text
      .verifying(emailaddressNotValidKey, email => isValidEmail(email) || email.length == 0)
      .verifying(emailMaxLengthKey, email => email.length <= 320)
      .verifying(emailaddressRequiredKey, email => email.length > 0)
  }

  val loginPasswordValidator =
    Forms.text.verifying(loginPasswordRequiredKey, isNotBlankString)

  private val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
  private def isValidEmail(email: String): Boolean = emailRegex.findFirstMatchIn(email).isDefined
  private def isNotBlankString: String => Boolean = s => s.trim.length > 0
}
