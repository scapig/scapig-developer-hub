package services

import connectors.{ApiDefinitionConnector, ApplicationConnector}
import models.{APIVersionSubscription, _}
import org.joda.time.DateTime
import org.mockito.BDDMockito
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import utils.UnitSpec

import scala.concurrent.Future
import scala.concurrent.Future.successful

class ApplicationServiceSpec extends UnitSpec with MockitoSugar {

  val collaboratorEmail = "admin@app.com"
  val applicationUrls = ApplicationUrls(Seq("http://redirecturi"), "http://conditionUrl", "http://privacyUrl")
  val prodCredentials = EnvironmentCredentials("prodClientId", "prodServerToken", Seq(ClientSecret("prodSecret", DateTime.now())))
  val sandboxCredentials = EnvironmentCredentials("sandboxClientId", "sandboxServerToken", Seq(ClientSecret("sandboxSecret", DateTime.now())))
  val application = Application("app name", "app description", Set(Collaborator(collaboratorEmail, Role.ADMINISTRATOR)), applicationUrls,
    ApplicationCredentials(prodCredentials, sandboxCredentials), DateTime.now(), RateLimitTier.BRONZE)

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
    val api1 = APIDefinition("api1", "apiContext1", Seq(APIVersion("v1", APIStatus.PUBLISHED), APIVersion("v2", APIStatus.PROTOTYPED)))
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
  }
}
