package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import config.DefaultEnv
import models.ApiNotFoundException
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}
import services.{ApiDefinitionService, RamlService, SessionService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ApiDocumentationController @Inject()(cc: ControllerComponents,
                                           apiDefinitionService: ApiDefinitionService,
                                           sessionService: SessionService,
                                           ramlService: RamlService,
                                           silhouette: Silhouette[DefaultEnv]) extends AbstractController(cc) with I18nSupport {

  def listApis() = silhouette.UserAwareAction.async { implicit request =>
    for {
      apis <- apiDefinitionService.fetchAllApis()
    } yield Ok(views.html.documentation.apisList(apis, request.identity))
  }

  def getApi(context: String, version: Option[String]) = silhouette.UserAwareAction.async { implicit request =>
    (for {
      api <- apiDefinitionService.fetchApi(context)
      selectedVersion = version.getOrElse(api.versions.head.version)
      raml <- ramlService.fetchRaml(context, selectedVersion)
    } yield Ok(views.html.documentation.api(api, raml, request.identity))) recover {
      case _: ApiNotFoundException => NotFound
    }
  }

  def index() = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(Ok(views.html.index(request.identity)))
  }
}