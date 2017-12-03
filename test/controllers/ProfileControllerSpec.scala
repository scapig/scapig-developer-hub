package controllers

import java.util.UUID

import com.google.inject.{AbstractModule, TypeLiteral}
import com.mohiva.play.silhouette.api.{Environment, LoginInfo, Silhouette}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test.FakeEnvironment
import config.DefaultEnv
import models._
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Mockito.verify
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.CSRFTokenHelper._
import play.api.test.{FakeRequest, Helpers}
import services.{ApplicationService, SessionService}
import utils.UnitSpec
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}
import com.mohiva.play.silhouette.test._

class ProfileControllerSpec extends UnitSpec with MockitoSugar {

  val userEmail = "admin@app.com"
  val developer = Developer(userEmail, "John", "Doe")

  val loginInfo = LoginInfo(CredentialsProvider.ID, UUID.randomUUID().toString)
  implicit val env: FakeEnvironment[DefaultEnv] = FakeEnvironment[DefaultEnv](Seq(loginInfo -> Developer(userEmail, "John", "Doe")))

  class FakeModule extends AbstractModule {
    def configure(): Unit = {
      bind(new TypeLiteral[Environment[DefaultEnv]]{}).toInstance(env)
    }
  }

  trait Setup {
    val sessionService = mock[SessionService]

    val playApplication = new GuiceApplicationBuilder()
      .overrides(new FakeModule())
      .bindings(inject.bind[SessionService].to(sessionService))
      .build()

    val authenticatedRequest = FakeRequest().withAuthenticator[DefaultEnv](loginInfo)
    val unauthenticatedRequest = FakeRequest()

    val underTest = playApplication.injector.instanceOf[ProfileController]
  }

  "showProfileForm" should {
    "display the user profile form" in new Setup {
      given(sessionService.fetchDeveloper(userEmail)).willReturn(successful(developer))

      val result = await(underTest.showProfileForm()(addCSRFToken(authenticatedRequest)))

      status(result) shouldBe Status.OK
      bodyOf(result) should (include("John") and include("Doe"))
    }

    "redirect to the login page when not logged in" in new Setup {

      val result = await(underTest.showProfileForm()(addCSRFToken(unauthenticatedRequest)))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/developer/login"
    }
  }

  "editProfileAction" should {
    "save the profile and refresh to the profile page" in new Setup {
      given(sessionService.updateUserProfile(any(), any())).willReturn(successful(HasSucceeded))

      val result = await(underTest.editProfileAction()(addCSRFToken(authenticatedRequest.withFormUrlEncodedBody(
        "firstName" -> "newFirstName",
        "lastName" -> "newLastName"
      ))))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/developer/profile?saved=true"
      verify(sessionService).updateUserProfile(userEmail, UserProfileEditRequest("newFirstName", "newLastName"))
    }

    "redirect to the login page when not logged in" in new Setup {

      val result = await(underTest.editProfileAction()(addCSRFToken(unauthenticatedRequest.withFormUrlEncodedBody(
        "firstName" -> "newFirstName",
        "lastName" -> "newLastName"
      ))))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/developer/login"
    }
  }

  "showRegistrationForm" should {
    "display the user registration form" in new Setup {
      val result = await(underTest.showRegistrationForm()(addCSRFToken(unauthenticatedRequest)))

      status(result) shouldBe Status.OK
      bodyOf(result) should include("Register")
    }
  }

  "registerAction" should {
    "save the user and redirect to the user profile" in new Setup {
      given(sessionService.register(any())).willReturn(successful(developer))

      val result = await(underTest.registerAction()(addCSRFToken(unauthenticatedRequest.withFormUrlEncodedBody(
        "email" -> userEmail,
        "firstName" -> "John",
        "lastName" -> "Doe",
        "password" -> "Password",
        "confirmPassword" -> "Password"
      ))))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/developer/login?registered=true"
      verify(sessionService).register(UserCreateRequest(userEmail, "Password", "John", "Doe"))
    }

    "validate that the 2 passwords are similar" in new Setup {
      given(sessionService.register(any())).willReturn(successful(developer))

      val result = await(underTest.registerAction()(addCSRFToken(unauthenticatedRequest.withFormUrlEncodedBody(
        "email" -> userEmail,
        "firstName" -> "John",
        "lastName" -> "Doe",
        "password" -> "Password",
        "confirmPassword" -> "anotherPassword"
      ))))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include ("Your passwords do not match")
    }

    "return an error message when the user is already registered" in new Setup {
      given(sessionService.register(any())).willReturn(failed(UserAlreadyRegisteredException()))

      val result = await(underTest.registerAction()(addCSRFToken(unauthenticatedRequest.withFormUrlEncodedBody(
        "email" -> userEmail,
        "firstName" -> "John",
        "lastName" -> "Doe",
        "password" -> "Password",
        "confirmPassword" -> "Password"
      ))))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include ("Choose an email address that is not already registered")
    }

  }

  "showChangePasswordForm" should {
    "display the change password form" in new Setup {
      val result = await(underTest.showChangePasswordForm()(addCSRFToken(authenticatedRequest)))

      status(result) shouldBe Status.OK
      bodyOf(result) should include("Change Password")
    }

    "redirect to the login page when not logged in" in new Setup {
      val result = await(underTest.showChangePasswordForm()(addCSRFToken(unauthenticatedRequest)))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/developer/login"
    }
  }

  "changePasswordAction" should {
    "update the password and redirect to the user profile" in new Setup {
      given(sessionService.changePassword(any(), any())).willReturn(successful(HasSucceeded))

      val result = await(underTest.changePasswordAction()(addCSRFToken(authenticatedRequest.withFormUrlEncodedBody(
        "oldPassword" -> "OldPassword",
        "password" -> "Password",
        "confirmPassword" -> "Password"
      ))))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/developer/profile?saved=true"
      verify(sessionService).changePassword(userEmail, ChangePasswordRequest("OldPassword", "Password"))
    }

    "validate that the password and confirmPassword are similar" in new Setup {
      given(sessionService.changePassword(any(), any())).willReturn(successful(HasSucceeded))

      val result = await(underTest.changePasswordAction()(addCSRFToken(authenticatedRequest.withFormUrlEncodedBody(
        "oldPassword" -> "OldPassword",
        "password" -> "Password",
        "confirmPassword" -> "anotherPassword"
      ))))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include ("Your passwords do not match")
    }

    "return an error message when the password is not valid" in new Setup {
      given(sessionService.changePassword(any(), any())).willReturn(failed(InvalidCredentialsException()))

      val result = await(underTest.changePasswordAction()(addCSRFToken(authenticatedRequest.withFormUrlEncodedBody(
        "oldPassword" -> "OldPassword",
        "password" -> "Password",
        "confirmPassword" -> "Password"
      ))))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include ("Invalid password")
    }

    "redirect to the login page when not logged in" in new Setup {
      val result = await(underTest.changePasswordAction()(addCSRFToken(unauthenticatedRequest.withFormUrlEncodedBody(
        "oldPassword" -> "OldPassword",
        "password" -> "Password",
        "confirmPassword" -> "Password"
      ))))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/developer/login"
    }

  }

}