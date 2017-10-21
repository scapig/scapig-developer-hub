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

class LoginController @Inject()(val appConfig: AppConfig, val sessionService: SessionService, val messagesApi: MessagesApi) {
  val loginForm: Form[LoginForm] = LoginForm.form

  def showLoginPage() = Action.async { implicit request =>
    Future.successful(Results.Ok(views.html.signIn("Sign in", loginForm)))
  }

  def login() = Action.async { implicit request =>
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