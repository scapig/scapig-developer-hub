package connectors

import javax.inject.Inject

import config.AppConfig
import models.JsonFormatters._
import models._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeveloperConnector @Inject()(appConfig: AppConfig, wsClient: WSClient) {

  val serviceUrl = appConfig.serviceUrl("tapi-developer")

  def register(userCreateRequest: UserCreateRequest): Future[Developer] = {
    wsClient.url(s"$serviceUrl/developer").post(Json.toJson(userCreateRequest)) map {
      case response if response.status == Status.CREATED => Json.parse(response.body).as[Developer]
      case response if response.status == Status.CONFLICT => throw UserAlreadyRegisteredException()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from tapi-developer ${r.status} ${r.body}")
    }
  }

  def createSession(sessionCreateRequest: SessionCreateRequest): Future[SessionResponse] = {
    wsClient.url(s"$serviceUrl/session").post(Json.toJson(sessionCreateRequest)) map {
      case response if response.status == Status.CREATED => Json.parse(response.body).as[SessionResponse]
      case response if response.status == Status.UNAUTHORIZED => throw InvalidCredentialsException()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from tapi-developer ${r.status} ${r.body}")
    }
  }

  def fetchSession(sessionId: String): Future[SessionResponse] = {
    wsClient.url(s"$serviceUrl/session/$sessionId").get() map {
      case response if response.status == Status.OK => Json.parse(response.body).as[SessionResponse]
      case r: WSResponse => throw new RuntimeException(s"Invalid response from tapi-developer ${r.status} ${r.body}")
    }
  }

  def fetchDeveloper(email: String): Future[Developer] = {
    wsClient.url(s"$serviceUrl/developer?email=$email").get() map {
      case response if response.status == Status.OK => Json.parse(response.body).as[Developer]
      case r: WSResponse => throw new RuntimeException(s"Invalid response from tapi-developer ${r.status} ${r.body}")
    }
  }

  def updateProfile(email: String, userProfileEditRequest: UserProfileEditRequest): Future[HasSucceeded] = {
    wsClient.url(s"$serviceUrl/developer/$email").post(Json.toJson(userProfileEditRequest)) map {
      case response if response.status == Status.OK => HasSucceeded
      case r: WSResponse => throw new RuntimeException(s"Invalid response from tapi-developer ${r.status} ${r.body}")
    }
  }

  def changePassword(email: String, changePasswordRequest: ChangePasswordRequest): Future[HasSucceeded] = {
    wsClient.url(s"$serviceUrl/developer/$email/password").post(Json.toJson(changePasswordRequest)) map {
      case response if response.status == Status.NO_CONTENT => HasSucceeded
      case response if response.status == Status.UNAUTHORIZED => throw InvalidCredentialsException()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from tapi-developer ${r.status} ${r.body}")
    }
  }

  def deleteSession(sessionId: String): Future[HasSucceeded] = {
    wsClient.url(s"$serviceUrl/session/$sessionId").delete() map {
      case response if response.status == Status.NO_CONTENT => HasSucceeded
      case r: WSResponse => throw new RuntimeException(s"Invalid response from tapi-developer ${r.status} ${r.body}")
    }
  }

}
