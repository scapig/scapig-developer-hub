package services

import javax.inject.{Inject, Singleton}

import connectors.PublisherConnector
import models.ApiNotFoundException
import org.raml.v2.api.model.v10.api.Api

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class RamlService @Inject()(publisherConnector: PublisherConnector, ramlLoader: StringRamlLoader) {
  type RAML = Api

  def fetchRaml(context: String, version: String): Future[RAML] = {
    for {
      ramlContent <- publisherConnector.fetchRaml(context, version)
      raml = ramlContent flatMap (ramlLoader.load(_).toOption)
    } yield raml.getOrElse(throw ApiNotFoundException())
  }
}
