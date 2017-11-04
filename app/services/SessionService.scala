package services

import javax.inject.{Inject, Singleton}

import connectors.DeveloperConnector
import models._

import scala.concurrent.Future

@Singleton
class SessionService @Inject()(developerConnector: DeveloperConnector) {

  def fetch(sessionId: String): Future[Option[Session]] = ???

  def fetchDeveloper(email: String): Future[Developer] = developerConnector.fetchDeveloper(email)

  def updateUserProfile(email: String, userProfileEditRequest: UserProfileEditRequest): Future[HasSucceeded] =
    developerConnector.updateProfile(email, userProfileEditRequest)

  def register(userCreateRequest: UserCreateRequest): Future[Developer] = developerConnector.register(userCreateRequest)
}
