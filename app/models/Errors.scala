package models

case class ApplicationNotFoundException() extends RuntimeException()
case class ApiNotFoundException() extends RuntimeException()
case class UserAlreadyRegisteredException() extends RuntimeException()
case class InvalidCredentialsException() extends RuntimeException()
case class UnauthorizedActionException() extends RuntimeException()

trait HasSucceeded
object HasSucceeded extends HasSucceeded