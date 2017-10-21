package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{AbstractController, Action, ControllerComponents, Results}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ManageApplicationController  @Inject()(cc: ControllerComponents) extends AbstractController(cc)  {

  def manageApps() = Action.async { implicit request =>
    Future(Results.Ok(""))
  }
}