package controllers

import models.Role.ADMINISTRATOR
import models.{ApplicationViewData, _}
import org.joda.time.DateTime
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.test.{FakeRequest, Helpers}
import services.ApplicationService
import utils.UnitSpec

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class ManageApplicationControllerSpec extends UnitSpec with MockitoSugar {

  val collaboratorEmail = "admin@app.com"
  val applicationUrls = ApplicationUrls(Seq("http://redirecturi"), "http://conditionUrl", "http://privacyUrl")
  val prodCredentials = EnvironmentCredentials("prodClientId", "prodServerToken", Seq(ClientSecret("prodSecret", DateTime.now())))
  val sandboxCredentials = EnvironmentCredentials("sandboxClientId", "sandboxServerToken", Seq(ClientSecret("sandboxSecret", DateTime.now())))
  val application = Application("app name", "app description", Set(Collaborator(collaboratorEmail, ADMINISTRATOR)), applicationUrls,
    ApplicationCredentials(prodCredentials, sandboxCredentials), DateTime.now(), RateLimitTier.BRONZE)
  val applicationId = application.id.toString

  val apiSubscription = APISubscription("api1", "apiContext1", Seq(APIVersionSubscription(APIVersion("v1", APIStatus.PROTOTYPED), subscribed = true)))
  val applicationViewData = ApplicationViewData(application, Seq(apiSubscription))

  trait Setup {
    val request = FakeRequest()
    val applicationService = mock[ApplicationService]

    val underTest = new ManageApplicationController(Helpers.stubControllerComponents(), applicationService)
  }

  "manageApps" should {
    "return the page listing all the applications" in new Setup {
      given(applicationService.fetchByCollaboratorEmail(collaboratorEmail)).willReturn(successful(Seq(application)))

      val result = await(underTest.manageApps()(request))

      status(result) shouldBe Status.OK
      bodyOf(result) should include(application.name)
    }
  }

  "editApplication" should {
    "display the application details" in new Setup {
      given(applicationService.fetchApplicationViewData(applicationId)).willReturn(successful(applicationViewData))

      val result = await(underTest.editApplication(applicationId)(request))

      status(result) shouldBe Status.OK
      bodyOf(result) should (include(application.name) and include(application.description))
    }

    "display the application subscriptions when tab is APP_SUBSCRIPTIONS_TAB" in new Setup {
      given(applicationService.fetchApplicationViewData(applicationId)).willReturn(successful(applicationViewData))

      val result = await(underTest.editApplication(applicationId, Some("APP_SUBSCRIPTIONS_TAB"))(request))

      status(result) shouldBe Status.OK
      bodyOf(result) should include(apiSubscription.apiName)
    }

    "display the application credentials when tab is APP_CREDENTIALS_TAB" in new Setup {
      given(applicationService.fetchApplicationViewData(applicationId)).willReturn(successful(applicationViewData))

      val result = await(underTest.editApplication(applicationId, Some("APP_CREDENTIALS_TAB"))(request))

      status(result) shouldBe Status.OK
      bodyOf(result) should (include(application.credentials.production.clientId) and include(application.credentials.sandbox.clientId))
    }

    "display application not found when the application does not exist" in new Setup {
      given(applicationService.fetchApplicationViewData(applicationId)).willReturn(failed(ApplicationNotFoundException()))

      val result = await(underTest.editApplication(applicationId)(request))

      status(result) shouldBe Status.NOT_FOUND
      bodyOf(result) should include("Application not found")
    }
  }

  "subscribe" should {
    "subscribe to the API and redirect to the application view" in new Setup {
      given(applicationService.subscribe(applicationId, "aContext", "v1")).willReturn(successful(HasSucceeded))

      val result = await(underTest.subscribe(applicationId, "aContext", "v1")(request))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers.get("Location") shouldBe Some(s"/applications/$applicationId?tab=APP_SUBSCRIPTIONS_TAB")
    }
  }

  "unsubscribe" should {
    "unsubscribe to the API and redirect to the application view" in new Setup {
      given(applicationService.unsubscribe(applicationId, "aContext", "v1")).willReturn(successful(HasSucceeded))

      val result = await(underTest.unsubscribe(applicationId, "aContext", "v1")(request))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers.get("Location") shouldBe Some(s"/applications/$applicationId?tab=APP_SUBSCRIPTIONS_TAB")
    }
  }

  "createApplicationForm" should {
    "display the create application form" in new Setup {
      val result = await(underTest.createApplicationForm()(request))

      status(result) shouldBe Status.OK
      bodyOf(result) should include("Add an application")
    }
  }

  "createApplicationAction" should {
    "create an application and redirect to the application details page" in new Setup {
      val createRequest = CreateApplicationRequest("appName", "appDescription", ApplicationUrls(), Set(Collaborator("admin@app.com", ADMINISTRATOR)))
      given(applicationService.createApplication(createRequest)).willReturn(successful(application))

      val result = await(underTest.createApplicationAction()(request.withFormUrlEncodedBody(
        "applicationName" -> "appName",
        "description" -> "appDescription"
      )))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe s"/applications/$applicationId"
    }

    "display the application form when the name is not set" in new Setup {
      val result = await(underTest.createApplicationAction()(request.withFormUrlEncodedBody(
        "applicationName" -> "",
        "description" -> "appDescription"
      )))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include("Add an application")
    }
  }

}