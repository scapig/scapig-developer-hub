package controllers

import javax.inject.{Inject, Singleton}

import models.UserProfileEditRequest
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
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
      Ok(views.html.userProfile(EditProfileForm.form.fill(EditProfileForm(developer.firstName, developer.lastName))))
    }
  }

  //TODO Add signed in
  def editProfileAction() = Action.async { implicit request =>
    val email = "admin@app.com"

    def editProfileWithFormErrors(errors: Form[EditProfileForm]) = {
      Future.successful(BadRequest(views.html.userProfile(errors)))
    }

    def editProfileWithValidForm(validForm: EditProfileForm) = {
      sessionService.updateUserProfile(email, UserProfileEditRequest(validForm.firstName, validForm.lastName))
        .map(_ => Redirect(routes.ProfileController.showProfileForm()))
    }

    EditProfileForm.form.bindFromRequest.fold(editProfileWithFormErrors, editProfileWithValidForm)
  }
}

case class EditProfileForm(firstName: String, lastName: String)

object EditProfileForm {

  val form: Form[EditProfileForm] = Form(
    mapping(
      "firstName" -> text,
      "lastName" -> text
    )(EditProfileForm.apply)(EditProfileForm.unapply)
  )
}
