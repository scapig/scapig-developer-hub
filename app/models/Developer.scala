package models

import scala.concurrent.Future

case class Developer(email: String, firstName: String, lastName: String) {
  val displayedName = s"$firstName $lastName"
}

case class Session(sessionId: String, developer: Developer)

case class AppAdmin(override val app: Future[Application]) extends UserStatus
case class AppCollaborator(override val app: Future[Application]) extends UserStatus

sealed trait UserStatus {
  val app: Future[Application]
}
