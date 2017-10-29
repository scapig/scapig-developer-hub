package controllers

import javax.inject.{Inject, Singleton}

import models.{ApplicationNotFoundException, ApplicationSummary, CreateApplicationRequest, UnauthorizedActionException}
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, Action, ControllerComponents, Results}
import services.ApplicationService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ManageApplicationController  @Inject()(cc: ControllerComponents, applicationService: ApplicationService) extends AbstractController(cc) with I18nSupport {

  val APP_DETAILS_TAB = "APP_DETAILS_TAB"
  val APP_SUBSCRIPTIONS_TAB = "APP_SUBSCRIPTIONS_TAB"
  val APP_CREDENTIALS_TAB = "APP_CREDENTIALS_TAB"

  //TODO Replace email by loggedIn action
  def manageApps() = Action.async { implicit request =>
    val email = "admin@app.com"
    applicationService.fetchByCollaboratorEmail(email) map { applications =>
      Ok(views.html.applications.manageApplications(applications.map(ApplicationSummary(_, email))))
    }
  }

  //TODO Check application collaborators
  def editApplication(id: String, tab: Option[String] = None) = Action.async { implicit request =>
    applicationService.fetchApplicationViewData(id) map { applicationViewData =>
      tab match {
        case Some(`APP_SUBSCRIPTIONS_TAB`) => Ok(views.html.applications.applicationSubscriptions(applicationViewData))
        case Some(`APP_CREDENTIALS_TAB`) => Ok(views.html.applications.applicationCredentials(applicationViewData))
        case _ => Ok(views.html.applications.applicationDetails(applicationViewData))
      }
    } recover {
      case _: ApplicationNotFoundException => Results.NotFound("Application not found")
    }
  }

  //TODO Check application collaborators
  def subscribe(id: String, context: String, version: String) = Action.async { implicit request =>
    applicationService.subscribe(id, context, version) map { _ =>
      Redirect(routes.ManageApplicationController.editApplication(id, Some(APP_SUBSCRIPTIONS_TAB)))
    }
  }

  //TODO Check application collaborators
  def unsubscribe(id: String, context: String, version: String) = Action.async { implicit request =>
    applicationService.unsubscribe(id, context, version) map { _ =>
      Redirect(routes.ManageApplicationController.editApplication(id, Some(APP_SUBSCRIPTIONS_TAB)))
    }
  }

  def createApplicationForm() = Action.async { implicit request =>
    Future.successful(Ok(views.html.applications.addApplication(AddApplicationForm.form)))
  }

  //TODO Add email
  def createApplicationAction() = Action.async { implicit request =>
    val email = "admin@app.com"

    def addApplicationWithFormErrors(errors: Form[AddApplicationForm]) = Future.successful(BadRequest(views.html.applications.addApplication(errors)))

    def addApplicationWithValidForm(validForm: AddApplicationForm) = {
      applicationService.createApplication(CreateApplicationRequest(validForm, email))
        .map(appCreated => Redirect(routes.ManageApplicationController.editApplication(appCreated.id.toString, None)))
    }
    AddApplicationForm.form.bindFromRequest.fold(addApplicationWithFormErrors, addApplicationWithValidForm)
  }
}

case class AddApplicationForm(applicationName: String, description: Option[String])

object AddApplicationForm {

  val form: Form[AddApplicationForm] = Form(
    mapping(
      "applicationName" -> applicationNameValidator,
      "description" -> optional(text)
    )(AddApplicationForm.apply)(AddApplicationForm.unapply)
  )
}
