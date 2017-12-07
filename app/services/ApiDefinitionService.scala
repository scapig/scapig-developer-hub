package services

import javax.inject.{Inject, Singleton}

import connectors.ApiDefinitionConnector
import models.APIDefinition

import scala.concurrent.Future

@Singleton
class ApiDefinitionService @Inject()(apiDefinitionConnector: ApiDefinitionConnector) {

  def fetchAllApis(): Future[Seq[APIDefinition]] = apiDefinitionConnector.fetchAllApis()

  def fetchApi(context: String): Future[APIDefinition] = apiDefinitionConnector.fetchApi(context)

}
