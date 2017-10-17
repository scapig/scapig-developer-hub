package config

import javax.inject.Singleton

import play.api.Logger
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.Future

@Singleton
class ErrorHandler extends DefaultHttpErrorHandler {
  override def onBadRequest(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(Results.BadRequest("Bad Request"))
  }

  override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(Results.NotFound("Page Not Found"))
  }

  override def onServerError(request: RequestHeader, exception: Throwable) = {
    Logger.error("An unexpected error occurred", exception)
    Future.successful(Results.InternalServerError("An unexpected error happened."))
  }

}