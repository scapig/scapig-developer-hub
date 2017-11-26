import java.net.URL

import controllers.FormKeys.{redirectUriInvalidKey, _}
import models.RateLimitTier
import play.api.data.Forms

import scala.util.Try

package object controllers {

  object FormKeys {
    val applicationNameInvalidKey = "application.name.invalid.field"
    val emailaddressRequiredKey = "emailaddress.error.required.field"
    val emailaddressNotValidKey = "emailaddress.error.not.valid.field"
    val emailAlreadyRegisteredKey = "emailaddress.already.registered.global"
    val emailMaxLengthKey = "emailaddress.error.maxLength.field"
    val loginPasswordRequiredKey = "loginpassword.error.required.field"
    val redirectUriInvalidKey = "redirect.uri.invalid.field"
    val rateLimitTierInvalidKey = "ratelimittier.invalid.field"
    val firstNameRequiredKey = "firstname.error.required.field"
    val lastNameRequiredKey = "lastname.error.required.field"
    val currentPasswordRequiredKey = "currentpassword.error.required.field"
    val newPasswordRequiredKey = "newpassword.error.required.field"
    val passwordRequiredKey = "password.error.required.field"
    val passwordInvalidKey = "password.error.invalid.field"
    val passwordNoMatchKey = "password.error.no.match.global"
    val invalidCredentialsKey = "credentials.error.invalid.field"
  }

  val emailValidator = {
    Forms.text
      .verifying(emailaddressNotValidKey, email => isValidEmail(email) || email.length == 0)
      .verifying(emailMaxLengthKey, email => email.length <= 320)
      .verifying(emailaddressRequiredKey, email => email.length > 0)
  }

  val loginPasswordValidator =
    Forms.text.verifying(loginPasswordRequiredKey, isNotBlankString)

  def requiredValidator(errorMessage: String) = Forms.text.verifying(errorMessage, isNotBlankString)

  private val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
  private def isValidEmail(email: String): Boolean = emailRegex.findFirstMatchIn(email).isDefined
  private def isNotBlankString: String => Boolean = s => s.trim.length > 0

  def applicationNameValidator =
    Forms.text.verifying(applicationNameInvalidKey,
      s => s.length >= 2 && s.length <= 50 && isAcceptedAscii(s))

  private def isAcceptedAscii(s: String) = !s.toCharArray.exists(c => 32 > c || c > 126)

  def redirectUriValidator = Forms.text.verifying(redirectUriInvalidKey,
    s => s.length == 0 || isValidUrl(s))

  def rateLimitTierValidator = Forms.text.verifying(rateLimitTierInvalidKey,
    tier => RateLimitTier.values.exists(_.toString == tier))

  private def isValidUrl: String => Boolean = s => Try(new URL(s.trim)).map(_ => true).getOrElse(false)
}
