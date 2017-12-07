package controllers

import java.util.UUID

import com.google.inject.{AbstractModule, TypeLiteral}
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test.{FakeEnvironment, _}
import config.DefaultEnv
import models._
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import services.{ApiDefinitionService, ClasspathRamlLoader, RamlService}
import utils.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.{failed, successful}

class ApiDocumentationControllerSpec extends UnitSpec with MockitoSugar {

  val context = "calendar"
  val version1 = "1.0"
  val version2 = "2.0"
  val api = APIDefinition("Calendar API", "My Calendar API", context,
    Seq(
      APIVersion(version2, APIStatus.PUBLISHED, Seq(Endpoint("/hello", "endpointDescription", HttpMethod.GET, AuthType.NONE))),
      APIVersion(version1, APIStatus.PUBLISHED, Seq(Endpoint("/hello", "endpointDescription", HttpMethod.GET, AuthType.NONE)))
    ))

  val raml = new ClasspathRamlLoader().load("calendar.raml").get
  val developer = Developer("john.doe@test.com", "John", "Doe")

  val loginInfo = LoginInfo(CredentialsProvider.ID, UUID.randomUUID().toString)
  implicit val env: FakeEnvironment[DefaultEnv] = FakeEnvironment[DefaultEnv](Seq(loginInfo -> developer))

  class FakeModule extends AbstractModule {
    def configure(): Unit = {
      bind(new TypeLiteral[Environment[DefaultEnv]]{}).toInstance(env)
    }
  }

  trait Setup {
    val apiDefinitionService = mock[ApiDefinitionService]
    val ramlService = mock[RamlService]

    val playApplication = new GuiceApplicationBuilder()
      .overrides(new FakeModule())
      .bindings(inject.bind[ApiDefinitionService].to(apiDefinitionService))
      .bindings(inject.bind[RamlService].to(ramlService))
      .build()

    val underTest = playApplication.injector.instanceOf[ApiDocumentationController]

    val authenticatedRequest = FakeRequest().withAuthenticator[DefaultEnv](loginInfo)
    val unauthenticatedRequest = FakeRequest()
  }

  "listApis" should {
    "display the list of APIs" in new Setup {
      given(apiDefinitionService.fetchAllApis()).willReturn(successful(Seq(api)))

      val result = await(underTest.listApis()(unauthenticatedRequest))

      status(result) shouldBe Status.OK
      bodyOf(result) should (include ("Calendar API") and include ("My Calendar API") and include (version2))
    }

    "display the user name in the banner when logged in" in new Setup {
      given(apiDefinitionService.fetchAllApis()).willReturn(successful(Seq(api)))

      val result = await(underTest.listApis()(authenticatedRequest))

      status(result) shouldBe Status.OK
      bodyOf(result) should include ("John Doe")
    }
  }

  "getApi" should {
    "display the API Documentation" in new Setup {
      given(apiDefinitionService.fetchApi(context)).willReturn(successful(api))
      given(ramlService.fetchRaml(context, version2)).willReturn(successful(raml))

      val result = await(underTest.getApi(context, Some(version2))(unauthenticatedRequest))

      status(result) shouldBe Status.OK
      bodyOf(result) should (include ("Calendar API") and include ("2.0"))
    }

    "default to the first API Version if not set" in new Setup {
      given(apiDefinitionService.fetchApi(context)).willReturn(successful(api))
      given(ramlService.fetchRaml(context, version2)).willReturn(successful(raml))

      val result = await(underTest.getApi(context, None)(unauthenticatedRequest))

      status(result) shouldBe Status.OK
      bodyOf(result) should (include ("Calendar API") and include ("2.0"))
    }

    "display the user name in the banner when logged in" in new Setup {
      given(apiDefinitionService.fetchApi(context)).willReturn(successful(api))
      given(ramlService.fetchRaml(context, version2)).willReturn(successful(raml))

      val result = await(underTest.getApi(context, None)(authenticatedRequest))

      status(result) shouldBe Status.OK
      bodyOf(result) should include ("John Doe")
    }

    "return not found when the API does not exist" in new Setup {
      given(apiDefinitionService.fetchApi(context)).willReturn(failed(ApiNotFoundException()))

      val result = await(underTest.getApi(context, Some(version2))(unauthenticatedRequest))

      status(result) shouldBe Status.NOT_FOUND
    }

    "return not found when the RAML does not exist" in new Setup {
      given(apiDefinitionService.fetchApi(context)).willReturn(successful(api))
      given(ramlService.fetchRaml(context, version2)).willReturn(failed(ApiNotFoundException()))

      val result = await(underTest.getApi(context, Some(version2))(unauthenticatedRequest))

      status(result) shouldBe Status.NOT_FOUND
    }

  }
}
