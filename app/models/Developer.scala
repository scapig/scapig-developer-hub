package models

import java.util.UUID

import scala.concurrent.Future

case class Developer(email: String, firstName: String, lastName: String) {
  val displayedName = s"$firstName $lastName"
}

case class Session(userEmail: String,
                   sessionId: String = UUID.randomUUID().toString)


case class AppAdmin(override val app: Future[Application]) extends UserStatus
case class AppCollaborator(override val app: Future[Application]) extends UserStatus

sealed trait UserStatus {
  val app: Future[Application]
}

case class UserCreateRequest(email: String,
                             password: String,
                             firstName: String,
                             lastName: String)
