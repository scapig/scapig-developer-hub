package services

import javax.inject.{Inject, Singleton}

import connectors.DeveloperConnector
import controllers.ChangePasswordForm
import models._

import scala.concurrent.Future

@Singleton
class SessionService @Inject()(developerConnector: DeveloperConnector) {

  def fetch(sessionId: String): Future[Option[Session]] = ???

  def fetchDeveloper(email: String): Future[Developer] = developerConnector.fetchDeveloper(email)

  def updateUserProfile(email: String, userProfileEditRequest: UserProfileEditRequest): Future[HasSucceeded] =
    developerConnector.updateProfile(email, userProfileEditRequest)

  def register(userCreateRequest: UserCreateRequest): Future[Developer] = developerConnector.register(userCreateRequest)

  def changePassword(email: String, changePasswordRequest: ChangePasswordRequest): Future[HasSucceeded] =
    developerConnector.changePassword(email, changePasswordRequest)
}
