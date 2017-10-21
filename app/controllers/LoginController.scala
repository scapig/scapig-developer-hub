package controllers

import javax.inject.{Inject, Singleton}

import config.AppConfig
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AbstractController, Action, ControllerComponents, Results}
import services.SessionService
import play.api.data.Forms._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.i18n.Messages.Implicits._

class LoginController @Inject()(val appConfig: AppConfig, val sessionService: SessionService, val messagesApi: MessagesApi) extends LoggedOut with I18nSupport {
  val loginForm: Form[LoginForm] = LoginForm.form

  def showLoginPage() = loggedOutAction { implicit request =>
    Future.successful(Ok(views.html.signIn("Sign in", loginForm)))
  }

  def login() = loggedOutAction { implicit request =>
    Future(Results.Ok(""))
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