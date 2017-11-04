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

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

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

  "showRegistrationForm" should {
    "display the user registration form" in new Setup {
      val result = await(underTest.showRegistrationForm()(addCSRFToken(request)))

      status(result) shouldBe Status.OK
      bodyOf(result) should include("Register")
    }
  }

  "registerAction" should {
    "save the user and redirect to the user profile" in new Setup {
      given(sessionService.register(any())).willReturn(successful(developer))

      val result = await(underTest.registerAction()(addCSRFToken(request.withFormUrlEncodedBody(
        "email" -> userEmail,
        "firstName" -> "John",
        "lastName" -> "Doe",
        "password" -> "Password",
        "confirmPassword" -> "Password"
      ))))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/profile"
      verify(sessionService).register(UserCreateRequest(userEmail, "Password", "John", "Doe"))
    }

    "validate that the 2 passwords are similar" in new Setup {
      given(sessionService.register(any())).willReturn(successful(developer))

      val result = await(underTest.registerAction()(addCSRFToken(request.withFormUrlEncodedBody(
        "email" -> userEmail,
        "firstName" -> "John",
        "lastName" -> "Doe",
        "password" -> "Password",
        "confirmPassword" -> "anotherPassword"
      ))))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include ("password.error.no.match.global")
    }

    "return an error message when the user is already registered" in new Setup {
      given(sessionService.register(any())).willReturn(failed(UserAlreadyRegisteredException()))

      val result = await(underTest.registerAction()(addCSRFToken(request.withFormUrlEncodedBody(
        "email" -> userEmail,
        "firstName" -> "John",
        "lastName" -> "Doe",
        "password" -> "Password",
        "confirmPassword" -> "Password"
      ))))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include ("emailaddress.already.registered.global")
    }

  }

}