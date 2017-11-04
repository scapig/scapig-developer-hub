package services

import connectors.DeveloperConnector
import models.{Developer, HasSucceeded, UserProfileEditRequest}
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import utils.UnitSpec

import scala.concurrent.Future.successful

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
}
