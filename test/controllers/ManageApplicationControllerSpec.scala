package controllers

import java.util.UUID

import com.google.inject.{AbstractModule, TypeLiteral}
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test._
import config.DefaultEnv
import models.Role.ADMINISTRATOR
import models.{ApplicationViewData, _}
import org.joda.time.DateTime
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import services.ApplicationService
import utils.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.{failed, successful}

class ManageApplicationControllerSpec extends UnitSpec with MockitoSugar {

  val collaboratorEmail = "admin@app.com"
  val prodCredentials = EnvironmentCredentials("prodClientId", "prodServerToken", Seq(ClientSecret("prodSecret", DateTime.now())))
  val sandboxCredentials = EnvironmentCredentials("sandboxClientId", "sandboxServerToken", Seq(ClientSecret("sandboxSecret", DateTime.now())))
  val application = Application("app name", "app description", Set(Collaborator(collaboratorEmail, ADMINISTRATOR)), Seq("http://redirecturi"),
    ApplicationCredentials(prodCredentials, sandboxCredentials), DateTime.now(), RateLimitTier.BRONZE)
  val applicationId = application.id.toString

  val apiSubscription = APISubscription("api1", "apiContext1", Seq(APIVersionSubscription(APIVersion("v1", APIStatus.PROTOTYPED), subscribed = true)))
  val applicationViewData = ApplicationViewData(application, Seq(apiSubscription))

  val loginInfo = LoginInfo(CredentialsProvider.ID, UUID.randomUUID().toString)
  implicit val env: FakeEnvironment[DefaultEnv] = FakeEnvironment[DefaultEnv](Seq(loginInfo -> Developer(collaboratorEmail, "John", "Doe")))

  class FakeModule extends AbstractModule {
    def configure(): Unit = {
      bind(new TypeLiteral[Environment[DefaultEnv]]{}).toInstance(env)
    }
  }

  trait Setup {
    val applicationService = mock[ApplicationService]

    val playApplication = new GuiceApplicationBuilder()
      .overrides(new FakeModule())
      .bindings(inject.bind[ApplicationService].to(applicationService))
      .build()

    val authenticatedRequest = FakeRequest().withAuthenticator[DefaultEnv](loginInfo)
    val unauthenticatedRequest = FakeRequest()

    val underTest = playApplication.injector.instanceOf[ManageApplicationController]
  }

  "manageApps" should {
    "return the page listing all the applications of the logged in user" in new Setup {

      given(applicationService.fetchByCollaboratorEmail(collaboratorEmail)).willReturn(successful(Seq(application)))

      val result = await(underTest.manageApps()(authenticatedRequest))

      status(result) shouldBe Status.OK
      bodyOf(result) should include(application.name)
    }

    "redirect to the login page when not logged in" in new Setup {

      val result = await(underTest.manageApps()(unauthenticatedRequest))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/developer/login"
    }
  }

  "editApplication" should {
    "display the application details" in new Setup {
      given(applicationService.fetchById(applicationId)).willReturn(successful(application))
      given(applicationService.fetchApplicationViewData(applicationId)).willReturn(successful(applicationViewData))

      val result = await(underTest.editApplication(applicationId)(addCSRFToken(authenticatedRequest)))

      status(result) shouldBe Status.OK
      bodyOf(result) should (include(application.name) and include(application.description))
    }

    "redirect to the login page when the user is not logged in" in new Setup {
      given(applicationService.fetchById(applicationId)).willReturn(successful(application))
      given(applicationService.fetchApplicationViewData(applicationId)).willReturn(successful(applicationViewData))

      val result = await(underTest.editApplication(applicationId)(addCSRFToken(unauthenticatedRequest)))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/developer/login"
    }

    "display NotFound page when the application does not belong to the user" in new Setup {
      given(applicationService.fetchById(applicationId)).willReturn(successful(application.copy(collaborators = Set())))
      given(applicationService.fetchApplicationViewData(applicationId)).willReturn(successful(applicationViewData))

      val result = await(underTest.editApplication(applicationId)(addCSRFToken(authenticatedRequest)))

      status(result) shouldBe Status.NOT_FOUND
      bodyOf(result) should include("Page Not Found")
    }

    "display the application subscriptions when tab is APP_SUBSCRIPTIONS_TAB" in new Setup {
      given(applicationService.fetchById(applicationId)).willReturn(successful(application))
      given(applicationService.fetchApplicationViewData(applicationId)).willReturn(successful(applicationViewData))

      val result = await(underTest.editApplication(applicationId, Some("APP_SUBSCRIPTIONS_TAB"))(addCSRFToken(authenticatedRequest)))

      status(result) shouldBe Status.OK
      bodyOf(result) should include(apiSubscription.apiName)
    }

    "display the production credentials when tab is PRODUCTION_CREDENTIALS_TAB" in new Setup {
      given(applicationService.fetchById(applicationId)).willReturn(successful(application))
      given(applicationService.fetchApplicationViewData(applicationId)).willReturn(successful(applicationViewData))

      val result = await(underTest.editApplication(applicationId, Some("PRODUCTION_CREDENTIALS_TAB"))(authenticatedRequest))

      status(result) shouldBe Status.OK
      bodyOf(result) should (include(application.credentials.production.clientId) and not include application.credentials.sandbox.clientId)
    }

    "display the sandbox credentials when tab is SANDBOX_CREDENTIALS_TAB" in new Setup {
      given(applicationService.fetchById(applicationId)).willReturn(successful(application))
      given(applicationService.fetchApplicationViewData(applicationId)).willReturn(successful(applicationViewData))

      val result = await(underTest.editApplication(applicationId, Some("SANDBOX_CREDENTIALS_TAB"))(authenticatedRequest))

      status(result) shouldBe Status.OK
      bodyOf(result) should (include(application.credentials.sandbox.clientId) and not include application.credentials.production.clientId)
    }

    "display application not found when the application does not exist" in new Setup {
      given(applicationService.fetchById(applicationId)).willReturn(failed(ApplicationNotFoundException()))

      val result = await(underTest.editApplication(applicationId)(authenticatedRequest))

      status(result) shouldBe Status.NOT_FOUND
      bodyOf(result) should include("Page Not Found")
    }
  }

  "subscribe" should {
    "subscribe to the API and redirect to the application view" in new Setup {
      given(applicationService.fetchById(applicationId)).willReturn(successful(application))
      given(applicationService.subscribe(applicationId, "aContext", "v1")).willReturn(successful(HasSucceeded))

      val result = await(underTest.subscribe(applicationId, "aContext", "v1")(authenticatedRequest))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers.get("Location") shouldBe Some(s"/developer/applications/$applicationId?tab=APP_SUBSCRIPTIONS_TAB&saved=true")
    }

    "redirect to the login page when the user is not logged in" in new Setup {
      given(applicationService.fetchById(applicationId)).willReturn(successful(application))
      given(applicationService.subscribe(applicationId, "aContext", "v1")).willReturn(successful(HasSucceeded))

      val result = await(underTest.subscribe(applicationId, "aContext", "v1")(unauthenticatedRequest))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/developer/login"
    }

    "display Page Not Found page when the application does not belong to the user" in new Setup {
      given(applicationService.fetchById(applicationId)).willReturn(successful(application.copy(collaborators = Set())))
      given(applicationService.subscribe(applicationId, "aContext", "v1")).willReturn(successful(HasSucceeded))

      val result = await(underTest.subscribe(applicationId, "aContext", "v1")(authenticatedRequest))

      status(result) shouldBe Status.NOT_FOUND
      bodyOf(result) should include("Page Not Found")
    }

  }

  "unsubscribe" should {
    "unsubscribe to the API and redirect to the application view" in new Setup {
      given(applicationService.fetchById(applicationId)).willReturn(successful(application))
      given(applicationService.unsubscribe(applicationId, "aContext", "v1")).willReturn(successful(HasSucceeded))

      val result = await(underTest.unsubscribe(applicationId, "aContext", "v1")(authenticatedRequest))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers.get("Location") shouldBe Some(s"/developer/applications/$applicationId?tab=APP_SUBSCRIPTIONS_TAB&saved=true")
    }

    "redirect to the login page when the user is not logged in" in new Setup {
      given(applicationService.fetchById(applicationId)).willReturn(successful(application))
      given(applicationService.unsubscribe(applicationId, "aContext", "v1")).willReturn(successful(HasSucceeded))

      val result = await(underTest.unsubscribe(applicationId, "aContext", "v1")(unauthenticatedRequest))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/developer/login"
    }

    "display Page Not Found page when the application does not belong to the user" in new Setup {
      given(applicationService.fetchById(applicationId)).willReturn(successful(application.copy(collaborators = Set())))
      given(applicationService.unsubscribe(applicationId, "aContext", "v1")).willReturn(successful(HasSucceeded))

      val result = await(underTest.unsubscribe(applicationId, "aContext", "v1")(authenticatedRequest))

      status(result) shouldBe Status.NOT_FOUND
      bodyOf(result) should include("Page Not Found")
    }

  }

  "createApplicationForm" should {
    "display the create application form" in new Setup {
      val result = await(underTest.createApplicationForm()(addCSRFToken(authenticatedRequest)))

      status(result) shouldBe Status.OK
      bodyOf(result) should include("Add an application")
    }

    "redirect to the login page when the user is not logged in" in new Setup {
      val result = await(underTest.createApplicationForm()(addCSRFToken(unauthenticatedRequest)))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/developer/login"
    }

  }

  "createApplicationAction" should {
    val createRequest = CreateApplicationRequest("appName", "appDescription", Seq.empty, Set(Collaborator("admin@app.com", ADMINISTRATOR)))

    "create an application and redirect to the application details page" in new Setup {
      given(applicationService.createApplication(createRequest)).willReturn(successful(application))

      val result = await(underTest.createApplicationAction()(addCSRFToken(authenticatedRequest.withFormUrlEncodedBody(
        "applicationName" -> "appName",
        "description" -> "appDescription"
      ))))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe s"/developer/applications/$applicationId?saved=true"
    }

    "display the application form when the name is not set" in new Setup {
      val result = await(underTest.createApplicationAction()(addCSRFToken(authenticatedRequest.withFormUrlEncodedBody(
        "applicationName" -> "",
        "description" -> "appDescription"
      ))))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include("Add an application")
    }

    "redirect to the login page when the user is not logged in" in new Setup {
      val result = await(underTest.createApplicationAction()(addCSRFToken(unauthenticatedRequest.withFormUrlEncodedBody(
        "applicationName" -> "appName",
        "description" -> "appDescription"
      ))))

      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/developer/login"
    }

  }
}