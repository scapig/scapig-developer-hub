package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import com.mohiva.play.silhouette.api.{LoginEvent, LoginInfo, Silhouette}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import config.{AppConfig, DefaultEnv}
import models.{InvalidCredentialsException, UserProfileEditRequest}
import org.webjars.play.WebJarsUtil
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.SessionService
import play.api.data.Forms._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.i18n.Messages.Implicits._

class LoginController @Inject()(cc: ControllerComponents,
                                appConfig: AppConfig,
                                sessionService: SessionService,
                                silhouette: Silhouette[DefaultEnv])(implicit webJarsUtil: WebJarsUtil, assets: AssetsFinder) extends AbstractController(cc) with I18nSupport {
  val loginForm: Form[LoginForm] = LoginForm.form

  def showLoginPage(registered: Option[Boolean] = None) = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Results.Ok(views.html.signIn("Sign in", loginForm, registered)))
  }

  def login() = silhouette.UnsecuredAction.async { implicit request =>
    def loginWithFormErrors(errors: Form[LoginForm]) = {
      Future.successful(Results.BadRequest(views.html.signIn("Sign in", errors)))
    }

    def loginWithValidForm(validForm: LoginForm) = {
      (for {
          sessionResponse <- sessionService.login(validForm.emailaddress, validForm.password)
          authenticator <- silhouette.env.authenticatorService.create(LoginInfo(CredentialsProvider.ID, sessionResponse.session.sessionId))
          result <- silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
            silhouette.env.authenticatorService.embed(v, Results.Redirect(routes.ManageApplicationController.manageApps()))
          }
        } yield result) recover {
        case _: InvalidCredentialsException => Results.BadRequest(views.html.signIn("Sign in",
          LoginForm.form.fill(validForm).withGlobalError(FormKeys.invalidCredentialsKey)))
      }
    }

    LoginForm.form.bindFromRequest.fold(loginWithFormErrors, loginWithValidForm)
  }

  def logout() = silhouette.SecuredAction.async { implicit request =>
    silhouette.env.authenticatorService.discard(request.authenticator, Results.Redirect(routes.LoginController.showLoginPage(None)))
  }

}

case class LoginForm(emailaddress: String, password: String)

object LoginForm {
  val form: Form[LoginForm] = Form(
    mapping(
      "emailaddress" -> emailValidator,
      "password" -> loginPasswordValidator
    )(LoginForm.apply)(LoginForm.unapply)
  )

}