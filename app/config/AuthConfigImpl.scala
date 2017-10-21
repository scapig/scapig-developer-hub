package config

import javax.inject.Inject
/*
import controllers.routes
import jp.t2v.lab.play2.auth.{AsyncIdContainer, AuthConfig, CookieTokenAccessor, TransparentIdContainer}
import models._
import play.api.mvc.Results.Redirect
import play.api.mvc.{RequestHeader, Results}
import services.SessionService

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect._


trait AuthConfigImpl extends AuthConfig {
  val appConfig: AppConfig
  val sessionService: SessionService

  override type Id = String
  override type User = Developer
  override type Authority = UserStatus

  override implicit def idTag = classTag[Id]

  override def sessionTimeoutInSeconds = appConfig.sessionTimeout

  override def resolveUser(id: Id)(implicit context: ExecutionContext) = {
    sessionService.fetch(id).map(_.map(_.developer))
  }

  override def loginSucceeded(request: RequestHeader)(implicit context: ExecutionContext) = {
    val uri = request.session.get("access_uri").getOrElse(routes.ManageApplicationController.manageApps().url)
    successful(Redirect(uri).withNewSession)
  }

  override def logoutSucceeded(request: RequestHeader)(implicit context: ExecutionContext) = {
    successful(Redirect(appConfig.apiDocumentationFrontendUrl))
  }

  override def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext) = {
    Future.successful(Redirect(routes.LoginController.showLoginPage()))
  }

  override def authorizationFailed(request: RequestHeader, user: Developer, authority: Option[Authority])(implicit context: ExecutionContext) = {
    Future.successful(Results.NotFound(appConfig.notFoundPage()))
  }

  override def authorize(user: User, authority: Authority)(implicit context: ExecutionContext) = {
    def getRole(app: Future[Application], user: Developer) = app.map(_.collaborators.find(_.emailAddress == user.email))

    authority match {
      case AppAdmin(app) => getRole(app, user).map(_.exists(_.role == Role.ADMINISTRATOR))
      case AppCollaborator(app) => getRole(app, user).map(_.isDefined)
      case _ => Future.successful(true)
    }

  }

  override lazy val idContainer = AsyncIdContainer(new TransparentIdContainer[Id])

  override lazy val tokenAccessor = new CookieTokenAccessor(
    cookieSecureOption = true,
    cookieMaxAge = Some(sessionTimeoutInSeconds)
  )

}
*/