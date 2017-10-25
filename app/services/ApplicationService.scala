package services

import javax.inject.Inject

import connectors.ApplicationConnector
import models.Application

import scala.concurrent.Future

class ApplicationService @Inject()(applicationConnector: ApplicationConnector) {
  def fetchByCollaboratorEmail(email: String): Future[Seq[Application]] = {
    applicationConnector.fetchByCollaboratorEmail(email)
  }
}
