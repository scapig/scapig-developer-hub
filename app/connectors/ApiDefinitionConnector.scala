package connectors

import javax.inject.Inject

import config.AppConfig
import models.{APIDefinition, Application}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import models.JsonFormatters._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ApiDefinitionConnector @Inject()(appConfig: AppConfig, wsClient: WSClient) {

  val serviceUrl = appConfig.serviceUrl("tapi-definition")

  def fetchAllApis(): Future[Seq[APIDefinition]] = {
    wsClient.url(s"$serviceUrl/apis").get() map {
      case response if response.status == 200 => Json.parse(response.body).as[Seq[APIDefinition]]
      case r: WSResponse => throw new RuntimeException(s"Invalid response from tapi-definition ${r.status} ${r.body}")
    }
  }
}
