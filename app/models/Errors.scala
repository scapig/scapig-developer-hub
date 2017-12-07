package models

case class ApplicationNotFoundException() extends RuntimeException()
case class ApiNotFoundException() extends RuntimeException()
case class UserAlreadyRegisteredException() extends RuntimeException()
case class InvalidCredentialsException() extends RuntimeException()
case class UnauthorizedActionException() extends RuntimeException()

case class RamlParseException(msg: String) extends RuntimeException(msg)
case class RamlNotFoundException(msg: String) extends RuntimeException(msg)
case class RamlUnsupportedVersionException(msg: String) extends RuntimeException(msg)

trait HasSucceeded
object HasSucceeded extends HasSucceeded