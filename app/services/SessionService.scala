package services

import models.Session

import scala.concurrent.Future

class SessionService {

  def fetch(sessionId: String): Future[Option[Session]] = ???
}
