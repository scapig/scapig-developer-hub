package services

import javax.inject.Inject

import connectors.{ApiDefinitionConnector, ApplicationConnector}
import models.{APISubscription, APIVersionSubscription, Application, ApplicationViewData}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationService @Inject()(applicationConnector: ApplicationConnector, apiDefinitionConnector: ApiDefinitionConnector) {
  def fetchByCollaboratorEmail(email: String): Future[Seq[Application]] = {
    applicationConnector.fetchByCollaboratorEmail(email)
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
}
