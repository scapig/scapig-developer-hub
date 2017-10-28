package controllers

import javax.inject.{Inject, Singleton}

import models.ApplicationSummary
import play.api.mvc.{AbstractController, Action, ControllerComponents, Results}
import services.ApplicationService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ManageApplicationController  @Inject()(cc: ControllerComponents, applicationService: ApplicationService) extends AbstractController(cc)  {

  val APP_DETAILS_TAB = "APP_DETAILS_TAB"
  val APP_SUBSCRIPTIONS_TAB = "APP_SUBSCRIPTIONS_TAB"
  val APP_CREDENTIALS_TAB = "APP_CREDENTIALS_TAB"

  //TODO Replace email by loggedIn action
  def manageApps(email: String) = Action.async { implicit request =>
    applicationService.fetchByCollaboratorEmail(email) map { applications =>
      Ok(views.html.applications.manageApplications(applications.map(ApplicationSummary(_, email))))
    }
  }

  def editApplication(id: String, tab: Option[String] = None) =  Action.async { implicit request =>
    applicationService.fetchApplicationViewData(id) map { applicationViewData =>
      tab match {
        case Some(`APP_SUBSCRIPTIONS_TAB`) => Ok(views.html.applications.applicationSubscriptions(applicationViewData))
        case Some(`APP_CREDENTIALS_TAB`) => Ok(views.html.applications.applicationCredentials(applicationViewData))
        case _ => Ok(views.html.applications.applicationDetails(applicationViewData))
      }
    }
  }
}