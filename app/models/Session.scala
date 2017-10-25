package models

case class SessionCreateRequest(email: String, password: String)

case class SessionResponse(session: Session, user: Developer)
