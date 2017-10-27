package controllers

import models.{ApplicationViewData, _}
import org.joda.time.DateTime
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.test.{FakeRequest, Helpers}
import services.ApplicationService
import utils.UnitSpec

import scala.concurrent.Future.successful

class ManageApplicationControllerSpec extends UnitSpec with MockitoSugar {

  val collaboratorEmail = "admin@app.com"
  val applicationUrls = ApplicationUrls(Seq("http://redirecturi"), "http://conditionUrl", "http://privacyUrl")
  val prodCredentials = EnvironmentCredentials("prodClientId", "prodServerToken", Seq(ClientSecret("prodSecret", DateTime.now())))
  val sandboxCredentials = EnvironmentCredentials("sandboxClientId", "sandboxServerToken", Seq(ClientSecret("sandboxSecret", DateTime.now())))
  val application = Application("app name", "app description", Set(Collaborator(collaboratorEmail, Role.ADMINISTRATOR)), applicationUrls,
    ApplicationCredentials(prodCredentials, sandboxCredentials), DateTime.now(), RateLimitTier.BRONZE)

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

      val result = await(underTest.manageApps(collaboratorEmail)(request))

      status(result) shouldBe Status.OK
      bodyOf(result) should include(application.name)
    }
  }

  "editApplication" should {
    "display the application details" in new Setup {
      given(applicationService.fetchApplicationViewData(application.id.toString)).willReturn(successful(applicationViewData))

      val result = await(underTest.editApplication(application.id.toString)(request))

      status(result) shouldBe Status.OK
      bodyOf(result) should (include(application.name) and include (application.description))
    }
  }

}
