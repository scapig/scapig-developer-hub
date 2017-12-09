package connectors

import javax.inject.Inject

import config.AppConfig
import models._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.Future
import models.JsonFormatters._

import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationConnector @Inject()(appConfig: AppConfig, wsClient: WSClient) {

  val serviceUrl = appConfig.serviceUrl("scapig-application")

  def fetchByCollaboratorEmail(emailAddress: String): Future[Seq[Application]] = {
    wsClient.url(s"$serviceUrl/applications?collaboratorEmail=$emailAddress").get() map {
      case response if response.status == 200 => Json.parse(response.body).as[Seq[Application]]
      case r: WSResponse => throw new RuntimeException(s"Invalid response from application ${r.status} ${r.body}")
    }
  }

  def create(createApplicationRequest: CreateApplicationRequest): Future[Application] = {
    wsClient.url(s"$serviceUrl/application").post(Json.toJson(createApplicationRequest)) map {
      case response if response.status == Status.CREATED => Json.parse(response.body).as[Application]
      case r: WSResponse => throw new RuntimeException(s"Invalid response from application ${r.status} ${r.body}")
    }
  }

  def update(id: String, updateApplicationRequest: UpdateApplicationRequest): Future[Application] = {
    wsClient.url(s"$serviceUrl/application/$id").post(Json.toJson(updateApplicationRequest)) map {
      case response if response.status == Status.OK => Json.parse(response.body).as[Application]
      case r: WSResponse => throw new RuntimeException(s"Invalid response from application ${r.status} ${r.body}")
    }
  }

  def fetch(id: String): Future[Application] = {
    wsClient.url(s"$serviceUrl/application/$id").get() map {
      case response if response.status == Status.OK => Json.parse(response.body).as[Application]
      case response if response.status == Status.NOT_FOUND => throw ApplicationNotFoundException()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from application ${r.status} ${r.body}")
    }
  }

  def fetchSubscriptions(id: String): Future[Seq[APIIdentifier]] = {
    wsClient.url(s"$serviceUrl/application/$id/subscription").get() map {
      case response if response.status == Status.OK => Json.parse(response.body).as[Seq[APIIdentifier]]
      case response if response.status == Status.NOT_FOUND => throw ApplicationNotFoundException()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from application ${r.status} ${r.body}")
    }
  }

  def subscribeToApi(appId: String, apiIdentifier: APIIdentifier): Future[HasSucceeded] = {
    wsClient.url(s"$serviceUrl/application/$appId/subscription?context=${apiIdentifier.context}&version=${apiIdentifier.version}").post(Json.obj()) map {
      case response if response.status == Status.NO_CONTENT => HasSucceeded
      case response if response.status == Status.NOT_FOUND => throw ApplicationNotFoundException()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from application ${r.status} ${r.body}")
    }
  }

  def unsubscribeToApi(appId: String, apiIdentifier: APIIdentifier): Future[HasSucceeded] = {
    wsClient.url(s"$serviceUrl/application/$appId/subscription?context=${apiIdentifier.context}&version=${apiIdentifier.version}").delete() map {
      case response if response.status == Status.NO_CONTENT => HasSucceeded
      case response if response.status == Status.NOT_FOUND => throw ApplicationNotFoundException()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from application ${r.status} ${r.body}")
    }
  }

}
