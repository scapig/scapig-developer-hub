package controllers

import models._
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Mockito.verify
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.test.CSRFTokenHelper._
import play.api.test.{FakeRequest, Helpers}
import services.SessionService
import utils.UnitSpec

import scala.concurrent.Future.successful

class ProfileControllerSpec extends UnitSpec with MockitoSugar {

  val userEmail = "admin@app.com"
  val developer = Developer(userEmail, "John", "Doe")

  trait Setup {
    val request = FakeRequest()
    val sessionService = mock[SessionService]

    val underTest = new ProfileController(Helpers.stubControllerComponents(), sessionService)
  }

  "showProfileForm" should {
    "display the user profile form" in new Setup {
      given(sessionService.fetchDeveloper(userEmail)).willReturn(successful(developer))

      val result = await(underTest.showProfileForm()(addCSRFToken(request)))

      status(result) shouldBe Status.OK
      bodyOf(result) should (include("John") and include("Doe"))
    }
  }

  "editProfileAction" should {
    "save the profile and refresh to the profile page" in new Setup {
      given(sessionService.updateUserProfile(any(), any())).willReturn(successful(HasSucceeded))

      val result = await(underTest.editProfileAction()(addCSRFToken(request.withFormUrlEncodedBody(
        "firstName" -> "newFirstName",
        "lastName" -> "newLastName"
      ))))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/profile"
      verify(sessionService).updateUserProfile(userEmail, UserProfileEditRequest("newFirstName", "newLastName"))
    }
  }
}