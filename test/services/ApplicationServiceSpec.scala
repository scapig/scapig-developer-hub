package services

import connectors.{ApiDefinitionConnector, ApplicationConnector}
import models.{APIVersionSubscription, _}
import org.joda.time.DateTime
import org.mockito.{BDDMockito, Mockito}
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.scalatest.mockito.MockitoSugar
import utils.UnitSpec

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class ApplicationServiceSpec extends UnitSpec with MockitoSugar {

  val collaboratorEmail = "admin@app.com"
  val redirectUris = Seq("http://redirecturi")
  val prodCredentials = EnvironmentCredentials("prodClientId", "prodServerToken", Seq(ClientSecret("prodSecret", DateTime.now())))
  val sandboxCredentials = EnvironmentCredentials("sandboxClientId", "sandboxServerToken", Seq(ClientSecret("sandboxSecret", DateTime.now())))
  val application = Application("app name", "app description", Set(Collaborator(collaboratorEmail, Role.ADMINISTRATOR)), redirectUris,
    ApplicationCredentials(prodCredentials, sandboxCredentials), DateTime.now(), RateLimitTier.BRONZE)
  val applicationId = application.id.toString

  val api1 = APIDefinition("api1", "apiContext1", Seq(APIVersion("v1", APIStatus.PUBLISHED), APIVersion("v2", APIStatus.PROTOTYPED)))

  trait Setup {
    val applicationConnector = mock[ApplicationConnector]
    val apiDefinitionConnector = mock[ApiDefinitionConnector]

    val underTest = new ApplicationService(applicationConnector, apiDefinitionConnector)
  }

  "fetchByCollaboratorEmail" should {
    "return all the applications of the user" in new Setup {
      given(applicationConnector.fetchByCollaboratorEmail(collaboratorEmail)).willReturn(successful(Seq(application)))

      val result = await(underTest.fetchByCollaboratorEmail(collaboratorEmail))

      result shouldBe Seq(application)
    }
  }

  "fetchApplicationViewData" should {
    val api2 = APIDefinition("api2", "apiContext2", Seq(APIVersion("v1", APIStatus.PUBLISHED)))
    val subscribedApi = APIIdentifier("apiContext1", "v1")

    "return an ApplicationViewData with the subscribed APIs when the application exists" in new Setup {
      given(applicationConnector.fetch(application.id.toString)).willReturn(successful(application))
      given(applicationConnector.fetchSubscriptions(application.id.toString)).willReturn(successful(Seq(subscribedApi)))
      given(apiDefinitionConnector.fetchAllApis()).willReturn(successful(Seq(api1, api2)))

      val result = await(underTest.fetchApplicationViewData(application.id.toString))

      result shouldBe ApplicationViewData(application, Seq(
        APISubscription("api1", "apiContext1", Seq(
          APIVersionSubscription(APIVersion("v1", APIStatus.PUBLISHED), subscribed = true),
          APIVersionSubscription(APIVersion("v2", APIStatus.PROTOTYPED), subscribed = false)
        )),
        APISubscription("api2", "apiContext2", Seq(
          APIVersionSubscription(APIVersion("v1", APIStatus.PUBLISHED), subscribed = false)
        ))))
    }

    "fail with ApplicationNotFoundException when the application does not exist" in new Setup {
      given(applicationConnector.fetch(application.id.toString)).willReturn(failed(ApplicationNotFoundException()))

      intercept[ApplicationNotFoundException]{await(underTest.fetchApplicationViewData(application.id.toString))}
    }
  }

  "subscribe" should {
    "subscribe the application to the API" in new Setup {
      given(apiDefinitionConnector.fetchApi("aContext", "v1")).willReturn(successful(api1))
      given(applicationConnector.subscribeToApi(applicationId, APIIdentifier("aContext", "v1"))).willReturn(successful(HasSucceeded))

      val result = await(underTest.subscribe(applicationId, "aContext", "v1"))

      result shouldBe HasSucceeded
      verify(applicationConnector).subscribeToApi(applicationId, APIIdentifier("aContext", "v1"))
    }

    "fail with ApiNotFoundException when the API does not exist" in new Setup {
      given(apiDefinitionConnector.fetchApi("aContext", "v1")).willReturn(failed(ApiNotFoundException()))

      intercept[ApiNotFoundException] {
        await(underTest.subscribe(applicationId, "aContext", "v1"))
      }
    }

    "fail with ApplicationNotFoundException when the API does not exist" in new Setup {
      given(apiDefinitionConnector.fetchApi("aContext", "v1")).willReturn(successful(api1))
      given(applicationConnector.subscribeToApi(applicationId, APIIdentifier("aContext", "v1"))).willReturn(failed(ApplicationNotFoundException()))

      intercept[ApplicationNotFoundException] {
        await(underTest.subscribe(applicationId, "aContext", "v1"))
      }
    }
  }

  "unsubscribe" should {
    "unsubscribe the application to the API" in new Setup {
      given(apiDefinitionConnector.fetchApi("aContext", "v1")).willReturn(successful(api1))
      given(applicationConnector.unsubscribeToApi(applicationId, APIIdentifier("aContext", "v1"))).willReturn(successful(HasSucceeded))

      val result = await(underTest.unsubscribe(applicationId, "aContext", "v1"))

      result shouldBe HasSucceeded
      verify(applicationConnector).unsubscribeToApi(applicationId, APIIdentifier("aContext", "v1"))
    }

    "fail with ApiNotFoundException when the API does not exist" in new Setup {
      given(apiDefinitionConnector.fetchApi("aContext", "v1")).willReturn(failed(ApiNotFoundException()))

      intercept[ApiNotFoundException] {
        await(underTest.unsubscribe(applicationId, "aContext", "v1"))
      }
    }

    "fail with ApplicationNotFoundException when the API does not exist" in new Setup {
      given(apiDefinitionConnector.fetchApi("aContext", "v1")).willReturn(successful(api1))
      given(applicationConnector.unsubscribeToApi(applicationId, APIIdentifier("aContext", "v1"))).willReturn(failed(ApplicationNotFoundException()))

      intercept[ApplicationNotFoundException] {
        await(underTest.unsubscribe(applicationId, "aContext", "v1"))
      }
    }
  }

  "create" should {
    "create a new application" in new Setup {
      val createApplicationRequest = CreateApplicationRequest("appName", "appDescription", redirectUris, Set(Collaborator(collaboratorEmail, Role.ADMINISTRATOR)))

      given(applicationConnector.create(createApplicationRequest)).willReturn(successful(application))

      val result = await(underTest.createApplication(createApplicationRequest))

      result shouldBe application
    }
  }

  "update" should {
    "update an application" in new Setup {
      val updateApplicationRequest = UpdateApplicationRequest("appName", "appDescription", redirectUris, RateLimitTier.SILVER)

      given(applicationConnector.update(application.id.toString, updateApplicationRequest)).willReturn(successful(application))

      val result = await(underTest.updateApplication(application.id.toString, updateApplicationRequest))

      result shouldBe application
    }
  }

  "fetchById" should {
    "return the application" in new Setup {
      given(applicationConnector.fetch(application.id.toString)).willReturn(successful(application))

      val result = await(underTest.fetchById(application.id.toString))

      result shouldBe application
    }

    "propagate ApplicationNotFoundException when the application does not exist" in new Setup {
      given(applicationConnector.fetch(application.id.toString)).willReturn(failed(ApplicationNotFoundException()))

      intercept[ApplicationNotFoundException]{await(underTest.fetchById(application.id.toString))}
    }

  }
}
