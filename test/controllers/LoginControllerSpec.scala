package controllers

import java.util.UUID

import com.google.inject.{AbstractModule, TypeLiteral}
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test.{FakeEnvironment, _}
import config.DefaultEnv
import models.{Developer, InvalidCredentialsException, Session, SessionResponse}
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import services.SessionService
import utils.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.{failed, successful}

class LoginControllerSpec extends UnitSpec with MockitoSugar {

  val userEmail = "admin@app.com"
  val developer = Developer(userEmail, "John", "Doe")

  val loginInfo = LoginInfo(CredentialsProvider.ID, UUID.randomUUID().toString)
  implicit val env: FakeEnvironment[DefaultEnv] = FakeEnvironment[DefaultEnv](Seq(loginInfo -> developer))

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

    val underTest = playApplication.injector.instanceOf[LoginController]

    val authenticatedRequest = FakeRequest().withAuthenticator[DefaultEnv](loginInfo)
    val unauthenticatedRequest = FakeRequest()
  }

  "showLoginPage" should {
    "display the login page" in new Setup {
      val result = await(underTest.showLoginPage()(addCSRFToken(unauthenticatedRequest)))

      status(result) shouldBe Status.OK
      bodyOf(result) should include ("Login")
    }
  }

  "login" should {
    "redirect to the manage application page when successful" in new Setup {
      given(sessionService.login(userEmail, "aPassword")).willReturn(
        successful(SessionResponse(Session(userEmail), developer)))

      val result = await(underTest.login()(addCSRFToken(unauthenticatedRequest.withFormUrlEncodedBody(
        "emailaddress" -> userEmail,
        "password" -> "aPassword"
      ))))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/applications"
    }

    "display the login page when the emailaddress is not set" in new Setup {

      val result = await(underTest.login()(addCSRFToken(unauthenticatedRequest.withFormUrlEncodedBody(
        "emailaddress" -> "",
        "password" -> "aPassword"
      ))))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include ("Provide your email address")
    }

    "display the login page when the password is not set" in new Setup {

      val result = await(underTest.login()(addCSRFToken(unauthenticatedRequest.withFormUrlEncodedBody(
        "emailaddress" -> userEmail,
        "password" -> ""
      ))))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include ("Provide your password")
    }

    "display the login page when the credentials are invalid" in new Setup {
      given(sessionService.login(userEmail, "aPassword")).willReturn(failed(InvalidCredentialsException()))

      val result = await(underTest.login()(addCSRFToken(unauthenticatedRequest.withFormUrlEncodedBody(
        "emailaddress" -> userEmail,
        "password" -> "aPassword"))))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include ("Invalid username or password")
    }

  }

}
