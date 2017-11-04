package services

import javax.inject.{Inject, Singleton}

import connectors.DeveloperConnector
import models.{Developer, HasSucceeded, Session, UserProfileEditRequest}

import scala.concurrent.Future

@Singleton
class SessionService @Inject()(developerConnector: DeveloperConnector) {

  def fetch(sessionId: String): Future[Option[Session]] = ???

  def fetchDeveloper(email: String): Future[Developer] = developerConnector.fetchDeveloper(email)

  def updateUserProfile(email: String, userProfileEditRequest: UserProfileEditRequest): Future[HasSucceeded] =
    developerConnector.updateProfile(email, userProfileEditRequest)
}
