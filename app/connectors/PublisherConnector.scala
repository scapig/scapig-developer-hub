package connectors

import javax.inject.Inject

import config.AppConfig
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PublisherConnector @Inject()(appConfig: AppConfig, wsClient: WSClient) {

    val serviceUrl = appConfig.serviceUrl("scapig-publisher")

    def fetchRaml(context: String, version: String): Future[Option[String]] = {
        wsClient.url(s"$serviceUrl/raml?context=$context&version=$version").get() map {
            case response if response.status == 200 => Some(response.body)
            case response if response.status == 404 => None
            case r: WSResponse => throw new RuntimeException(s"Invalid response from scapig-publisher ${r.status} ${r.body}")
        }
    }
}
