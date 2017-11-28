package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import config.DefaultEnv
import models.Developer
import models.JsonFormatters._
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.Future.successful

@Singleton
class SessionController  @Inject()(cc: ControllerComponents,
                                   silhouette: Silhouette[DefaultEnv]) extends AbstractController(cc) with I18nSupport {

  def fetchSignedInDeveloper() = silhouette.UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(developer) => successful(Ok(Json.toJson(developer.asInstanceOf[Developer])))
      case None => successful(NotFound("User not logged in"))
    }
  }
}
