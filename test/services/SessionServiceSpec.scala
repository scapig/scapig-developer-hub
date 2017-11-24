package services

import connectors.DeveloperConnector
import models._
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import utils.UnitSpec

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class SessionServiceSpec extends UnitSpec with MockitoSugar {

  val developer = Developer("admin@test.com", "John", "Doe")

  trait Setup {
    val developerConnector = mock[DeveloperConnector]

    val underTest = new SessionService(developerConnector)
  }

  "fetchDeveloper" should {
    "return the developer" in new Setup {
      given(developerConnector.fetchDeveloper(developer.email)).willReturn(successful(developer))

      val result = await(underTest.fetchDeveloper(developer.email))

      result shouldBe developer
    }
  }

  "updateUserProfile" should {
    val userProfileEditRequest = UserProfileEditRequest("newFirstName", "newLastName")

    "update the user profile" in new Setup {
      given(developerConnector.updateProfile(developer.email, userProfileEditRequest)).willReturn(successful(HasSucceeded))

      val result = await(underTest.updateUserProfile(developer.email, userProfileEditRequest))

      result shouldBe HasSucceeded
    }
  }

  "register" should {
    val userCreateRequest = UserCreateRequest("admin@test.com", "password", "John", "Doe")

    "create the user" in new Setup {
      given(developerConnector.register(userCreateRequest)).willReturn(successful(developer))

      val result = await(underTest.register(userCreateRequest))

      result shouldBe developer
    }
  }

  "changePassword" should {
    val changePasswordRequest = ChangePasswordRequest("oldPassword", "newPassword")

    "update the password" in new Setup {
      given(developerConnector.changePassword(developer.email, changePasswordRequest)).willReturn(successful(HasSucceeded))

      val result = await(underTest.changePassword(developer.email, changePasswordRequest))

      result shouldBe HasSucceeded
    }
  }

  "login" should {
    val username = "admin@test.com"
    val password = "aPassword"

    "return the session when successful" in new Setup {
      val sessionResponse = SessionResponse(Session(username), developer)
      given(developerConnector.createSession(SessionCreateRequest(username, password))).willReturn(successful(sessionResponse))

      val result = await(underTest.login(username, password))

      result shouldBe sessionResponse
    }

    "propagate InvalidCredentialsException when not successful" in new Setup {
      given(developerConnector.createSession(SessionCreateRequest(username, password))).willReturn(failed(InvalidCredentialsException()))

      intercept[InvalidCredentialsException]{await(underTest.login(username, password))}
    }

  }

}
