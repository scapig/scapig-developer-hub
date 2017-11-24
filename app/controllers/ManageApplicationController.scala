package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import config.DefaultEnv
import models._
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, seq, text}
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, Action, ControllerComponents, Results}
import services.ApplicationService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ManageApplicationController  @Inject()(cc: ControllerComponents, applicationService: ApplicationService,
                                             silhouette: Silhouette[DefaultEnv]) extends AbstractController(cc) with I18nSupport {

  val APP_DETAILS_TAB = "APP_DETAILS_TAB"
  val APP_SUBSCRIPTIONS_TAB = "APP_SUBSCRIPTIONS_TAB"
  val PRODUCTION_CREDENTIALS_TAB = "PRODUCTION_CREDENTIALS_TAB"
  val SANDBOX_CREDENTIALS_TAB = "SANDBOX_CREDENTIALS_TAB"

  def manageApps() = silhouette.SecuredAction.async { implicit request =>
    applicationService.fetchByCollaboratorEmail(request.identity.email) map { applications =>
      Ok(views.html.applications.manageApplications(applications.map(ApplicationSummary(_, request.identity.email))))
    }
  }

  //TODO Check application collaborators
  def editApplication(id: String, tab: Option[String] = None) = silhouette.SecuredAction.async { implicit request =>
    applicationService.fetchApplicationViewData(id) map { applicationViewData =>
      tab match {
        case Some(`APP_SUBSCRIPTIONS_TAB`) => Ok(views.html.applications.applicationSubscriptions(applicationViewData))
        case Some(`PRODUCTION_CREDENTIALS_TAB`) => Ok(views.html.applications.productionCredentials(applicationViewData))
        case Some(`SANDBOX_CREDENTIALS_TAB`) => Ok(views.html.applications.sandboxCredentials(applicationViewData))
        case _ => Ok(views.html.applications.applicationDetails(applicationViewData, EditApplicationForm.form.fill(EditApplicationForm(applicationViewData.app))))
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

  def createApplicationForm() = silhouette.SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.applications.addApplication(AddApplicationForm.form)))
  }

  def createApplicationAction() = silhouette.SecuredAction.async { implicit request =>

    def addApplicationWithFormErrors(errors: Form[AddApplicationForm]) = {
      Future.successful(BadRequest(views.html.applications.addApplication(errors)))
    }

    def addApplicationWithValidForm(validForm: AddApplicationForm) = {
      applicationService.createApplication(CreateApplicationRequest(validForm, request.identity.email))
        .map(appCreated => Redirect(routes.ManageApplicationController.editApplication(appCreated.id.toString, None)))
    }
    AddApplicationForm.form.bindFromRequest.fold(addApplicationWithFormErrors, addApplicationWithValidForm)
  }

  //TODO Add email
  def updateApplicationAction(id: String) = silhouette.SecuredAction.async { implicit request =>
    def updateApplicationWithFormErrors(errors: Form[EditApplicationForm]) = {
      applicationService.fetchApplicationViewData(id) map { applicationViewData =>
        BadRequest(views.html.applications.applicationDetails(applicationViewData, errors))
      }
    }

    def updateApplicationWithValidForm(validForm: EditApplicationForm) = {
      applicationService.updateApplication(id, UpdateApplicationRequest(validForm))
        .map(appUpdated => Redirect(routes.ManageApplicationController.editApplication(appUpdated.id.toString, None)))
    }
    EditApplicationForm.form.bindFromRequest.fold(updateApplicationWithFormErrors, updateApplicationWithValidForm)
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

case class EditApplicationForm(applicationName: String,
                               description: Option[String] = None,
                               redirectUris: Seq[String] = Seq(),
                               rateLimitTier: String)

object EditApplicationForm {

  def apply(app: Application): EditApplicationForm = EditApplicationForm(app.name, Some(app.description), app.redirectUris,app.rateLimitTier.toString)

  val form: Form[EditApplicationForm] = Form(
    mapping(
      "applicationName" -> applicationNameValidator,
      "description" -> optional(text),
      "redirectUris" -> seq(redirectUriValidator),
      "rateLimitTier" -> rateLimitTierValidator
    )(EditApplicationForm.apply)(EditApplicationForm.unapply)
  )
}
