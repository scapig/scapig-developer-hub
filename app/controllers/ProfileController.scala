package controllers

import javax.inject.{Inject, Singleton}

import controllers.FormKeys.{emailAlreadyRegisteredKey, passwordInvalidKey, passwordNoMatchKey}
import models._
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}
import services.SessionService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ProfileController  @Inject()(cc: ControllerComponents, sessionService: SessionService) extends AbstractController(cc) with I18nSupport {

  //TODO Add signed in
  def showProfileForm() = Action.async { implicit request =>
    val email = "admin@app.com"
    sessionService.fetchDeveloper(email) map { developer =>
      Ok(views.html.user.userProfile(EditProfileForm.form.fill(EditProfileForm(developer.firstName, developer.lastName))))
    }
  }

  //TODO Add signed in
  def editProfileAction() = Action.async { implicit request =>
    val email = "admin@app.com"

    def editProfileWithFormErrors(errors: Form[EditProfileForm]) = {
      Future.successful(BadRequest(views.html.user.userProfile(errors)))
    }

    def editProfileWithValidForm(validForm: EditProfileForm) = {
      sessionService.updateUserProfile(email, UserProfileEditRequest(validForm.firstName, validForm.lastName))
        .map(_ => Redirect(routes.ProfileController.showProfileForm()))
    }

    EditProfileForm.form.bindFromRequest.fold(editProfileWithFormErrors, editProfileWithValidForm)
  }

  def showRegistrationForm() = Action.async { implicit request =>
    Future(Ok(views.html.user.register(RegisterForm.form)))
  }

  def registerAction() = Action.async { implicit request =>
    def registerWithFormErrors(errors: Form[RegisterForm]) = {
      Future.successful(BadRequest(views.html.user.register(errors)))
    }

    def registerWithValidForm(validForm: RegisterForm) = {
      sessionService.register(UserCreateRequest(validForm.email, validForm.password, validForm.firstName, validForm.lastName))
        .map(_ => Redirect(routes.ProfileController.showProfileForm())) recover {
        case _: UserAlreadyRegisteredException => BadRequest(views.html.user.register(RegisterForm.form.fill(validForm).withGlobalError(emailAlreadyRegisteredKey)))
      }
    }

    RegisterForm.form.bindFromRequest.fold(registerWithFormErrors, registerWithValidForm)
  }

  //TODO Add session
  def showChangePasswordForm() = Action.async { implicit request =>
    Future(Ok(views.html.user.changePassword(ChangePasswordForm.form)))
  }

  //TODO Add session
  def changePasswordAction() = Action.async { implicit request =>
    val email = "admin@app.com"

    def changePasswordWithFormErrors(errors: Form[ChangePasswordForm]) = {
      Future.successful(BadRequest(views.html.user.changePassword(errors)))
    }

    def changePasswordWithValidForm(validForm: ChangePasswordForm) = {
      sessionService.changePassword(email, ChangePasswordRequest(validForm.oldPassword, validForm.password))
        .map(_ => Redirect(routes.ProfileController.showProfileForm())) recover {
        case _: InvalidCredentialsException => BadRequest(views.html.user.changePassword(ChangePasswordForm.form.fill(validForm).withGlobalError(passwordInvalidKey)))
      }
    }

    ChangePasswordForm.form.bindFromRequest.fold(changePasswordWithFormErrors, changePasswordWithValidForm)
  }

}

case class EditProfileForm(firstName: String, lastName: String)

object EditProfileForm {

  val form: Form[EditProfileForm] = Form(
    mapping(
      "firstName" -> requiredValidator(FormKeys.firstNameRequiredKey),
      "lastName" -> requiredValidator(FormKeys.lastNameRequiredKey)
    )(EditProfileForm.apply)(EditProfileForm.unapply)
  )
}

trait ConfirmPassword {
  val password: String
  val confirmPassword: String
}

object ConfirmPassword {
  def passwordsMatch = Constraint[ConfirmPassword]("password") {
    case (rf) if rf.password != rf.confirmPassword => Invalid(ValidationError(passwordNoMatchKey))
    case _ => Valid
  }
}

case class ChangePasswordForm(oldPassword: String, password: String, confirmPassword: String) extends ConfirmPassword

object ChangePasswordForm {
  val form: Form[ChangePasswordForm] = Form(
    mapping(
      "oldPassword" -> requiredValidator(FormKeys.currentPasswordRequiredKey),
      "password" -> requiredValidator(FormKeys.newPasswordRequiredKey),
      "confirmPassword" -> text
    )(ChangePasswordForm.apply)(ChangePasswordForm.unapply).verifying(ConfirmPassword.passwordsMatch)
  )

}

case class RegisterForm(email: String, firstName: String, lastName: String, password: String, confirmPassword: String) extends ConfirmPassword

object RegisterForm {

  val form: Form[RegisterForm] = Form(
    mapping(
      "email" -> emailValidator,
      "firstName" -> requiredValidator(FormKeys.firstNameRequiredKey),
      "lastName" -> requiredValidator(FormKeys.lastNameRequiredKey),
      "password" -> requiredValidator(FormKeys.passwordRequiredKey),
      "confirmPassword" -> text
    )(RegisterForm.apply)(RegisterForm.unapply).verifying(ConfirmPassword.passwordsMatch)
  )
}
