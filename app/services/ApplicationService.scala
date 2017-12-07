package services

import javax.inject.Inject

import connectors.{ApiDefinitionConnector, ApplicationConnector}
import models._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationService @Inject()(applicationConnector: ApplicationConnector, apiDefinitionConnector: ApiDefinitionConnector) {
  def fetchByCollaboratorEmail(email: String): Future[Seq[Application]] = {
    applicationConnector.fetchByCollaboratorEmail(email)
  }

  def fetchById(applicationId: String): Future[Application] = {
    applicationConnector.fetch(applicationId)
  }

  def fetchApplicationViewData(applicationId: String): Future[ApplicationViewData] = {
    for {
      application <- applicationConnector.fetch(applicationId)
      subscribedApis <- applicationConnector.fetchSubscriptions(applicationId)
      apis <- apiDefinitionConnector.fetchAllApis()
    } yield {
      val apiSubscriptions = apis map { api =>
        val versions = api.versions map (APIVersionSubscription(api.context, _, subscribedApis))
        APISubscription(api.name, api.context, versions)
      }
      ApplicationViewData(application, apiSubscriptions)
    }
  }

  def subscribe(applicationId: String, context: String, version: String): Future[HasSucceeded] = {
    for {
      api <- apiDefinitionConnector.fetchApi(context)
      _ = if(!api.versions.exists(_.version == version)) throw ApiNotFoundException()
      hasSucceeded <- applicationConnector.subscribeToApi(applicationId, APIIdentifier(context, version))
    } yield hasSucceeded
  }

  def unsubscribe(applicationId: String, context: String, version: String): Future[HasSucceeded] = {
    for {
      api <- apiDefinitionConnector.fetchApi(context)
      _ = if(!api.versions.exists(_.version == version)) throw ApiNotFoundException()
      hasSucceeded <- applicationConnector.unsubscribeToApi(applicationId, APIIdentifier(context, version))
    } yield hasSucceeded
  }

  def createApplication(createApplicationRequest: CreateApplicationRequest): Future[Application] = {
    applicationConnector.create(createApplicationRequest)
  }

  def updateApplication(id: String, updateApplicationRequest: UpdateApplicationRequest): Future[Application] = {
    applicationConnector.update(id, updateApplicationRequest)
  }

}
