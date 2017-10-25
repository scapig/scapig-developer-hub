package models

case class ApplicationNotFoundException() extends RuntimeException()
case class UserAlreadyRegisteredException() extends RuntimeException()
case class InvalidCredentialsException() extends RuntimeException()

trait HasSucceeded
object HasSucceeded extends HasSucceeded