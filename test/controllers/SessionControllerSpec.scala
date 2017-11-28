package controllers

import java.util.UUID

import com.google.inject.{AbstractModule, TypeLiteral}
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test.{FakeEnvironment, _}
import config.DefaultEnv
import models.Developer
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import utils.UnitSpec
import scala.concurrent.ExecutionContext.Implicits.global
import models.JsonFormatters._

class SessionControllerSpec extends UnitSpec with MockitoSugar {

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
    val playApplication = new GuiceApplicationBuilder()
      .overrides(new FakeModule())
      .build()

    val underTest = playApplication.injector.instanceOf[SessionController]

    val authenticatedRequest = FakeRequest().withAuthenticator[DefaultEnv](loginInfo)
    val unauthenticatedRequest = FakeRequest()
  }

  "fetchSignedInDeveloper" should {
    "return 200 (Ok) with the signed in developer when logged in" in new Setup {
      val result = await(underTest.fetchSignedInDeveloper()(addCSRFToken(authenticatedRequest)))

      status(result) shouldBe Status.OK
      jsonBodyOf(result) shouldBe Json.toJson(developer)
    }

    "return 404 (NotFound) when not logged in" in new Setup {
      val result = await(underTest.fetchSignedInDeveloper()(addCSRFToken(unauthenticatedRequest)))

      status(result) shouldBe Status.NOT_FOUND
    }
  }
}
