package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import config.{AppConfig, DefaultEnv}
import org.webjars.play.WebJarsUtil
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.Future

@Singleton
class DocsController @Inject()(cc: ControllerComponents,
                               appConfig: AppConfig,
                               silhouette: Silhouette[DefaultEnv])(implicit webJarsUtil: WebJarsUtil, assets: AssetsFinder) extends AbstractController(cc) with I18nSupport {

  def example() = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(Ok(views.html.testing.example(request.identity, appConfig.gatewayBaseUrl)))
  }
}
