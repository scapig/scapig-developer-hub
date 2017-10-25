package controllers

import javax.inject.{Inject, Singleton}

import models.ApplicationSummary
import play.api.mvc.{AbstractController, Action, ControllerComponents, Results}
import services.ApplicationService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ManageApplicationController  @Inject()(cc: ControllerComponents, applicationService: ApplicationService) extends AbstractController(cc)  {

  //TODO Replace email by loggedIn action
  def manageApps(email: String) = Action.async { implicit request =>
    applicationService.fetchByCollaboratorEmail(email) map { applications =>
      Ok(views.html.applications.manageApplications(applications.map(ApplicationSummary(_, email))))
    }
  }

  def editApplication(id: String) =  Action.async { implicit request =>
    Future(Ok(""))
  }
}