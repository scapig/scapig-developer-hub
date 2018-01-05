package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import config.DefaultEnv
import controllers.FormKeys.{emailAlreadyRegisteredKey, passwordInvalidKey, passwordNoMatchKey}
import models._
import org.webjars.play.WebJarsUtil
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}
import services.SessionService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ProfileController  @Inject()(cc: ControllerComponents,
                                   sessionService: SessionService,
                                   silhouette: Silhouette[DefaultEnv])(implicit webJarsUtil: WebJarsUtil, assets: AssetsFinder) extends AbstractController(cc) with I18nSupport {

  def showProfileForm(saved: Option[Boolean] = None) = silhouette.SecuredAction.async { implicit request =>
    sessionService.fetchDeveloper(request.identity.email) map { developer =>
      Ok(views.html.user.userProfile(request.identity, EditProfileForm.form.fill(EditProfileForm(developer.firstName, developer.lastName)), saved))
    }
  }

  def editProfileAction() = silhouette.SecuredAction.async { implicit request =>

    def editProfileWithFormErrors(errors: Form[EditProfileForm]) = {
      Future.successful(BadRequest(views.html.user.userProfile(request.identity, errors)))
    }

    def editProfileWithValidForm(validForm: EditProfileForm) = {
      sessionService.updateUserProfile(request.identity.email, UserProfileEditRequest(validForm.firstName, validForm.lastName))
        .map(_ => Redirect(routes.ProfileController.showProfileForm(Some(true))))
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
        .map(_ => Redirect(routes.LoginController.showLoginPage(Some(true)))) recover {
        case _: UserAlreadyRegisteredException => BadRequest(views.html.user.register(RegisterForm.form.fill(validForm).withGlobalError(emailAlreadyRegisteredKey)))
      }
    }

    RegisterForm.form.bindFromRequest.fold(registerWithFormErrors, registerWithValidForm)
  }

  def showChangePasswordForm() = silhouette.SecuredAction.async { implicit request =>
    Future(Ok(views.html.user.changePassword(request.identity, ChangePasswordForm.form)))
  }

  def changePasswordAction() = silhouette.SecuredAction.async { implicit request =>

    def changePasswordWithFormErrors(errors: Form[ChangePasswordForm]) = {
      Future.successful(BadRequest(views.html.user.changePassword(request.identity, errors)))
    }

    def changePasswordWithValidForm(validForm: ChangePasswordForm) = {
      sessionService.changePassword(request.identity.email, ChangePasswordRequest(validForm.oldPassword, validForm.password))
        .map(_ => Redirect(routes.ProfileController.showProfileForm(Some(true)))) recover {
        case _: InvalidCredentialsException => BadRequest(views.html.user.changePassword(request.identity, ChangePasswordForm.form.fill(validForm).withGlobalError(passwordInvalidKey)))
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
