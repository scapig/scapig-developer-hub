package controllers

import play.api.mvc.{Action, Results}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ManageApplicationController {

  def manageApps() = Action.async { implicit request =>
    Future(Results.Ok(""))
  }
}