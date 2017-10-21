package controllers

import config.AuthConfigImpl
import jp.t2v.lab.play2.auth.OptionalAuthElement
import jp.t2v.lab.play2.stackc.RequestWithAttributes
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait LoggedOut extends Controller  with AuthConfigImpl with OptionalAuthElement {
  def loggedOutAction(f: RequestWithAttributes[AnyContent] => Future[Result]): Action[AnyContent] = {
    AsyncStack { implicit request =>
      loggedIn match {
        case Some(user) => loginSucceeded(request)
        case None => f(request)
      }
    }
  }

}
