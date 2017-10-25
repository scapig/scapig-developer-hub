package services

import connectors.ApplicationConnector
import models._
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
    val underTest = new ApplicationService(applicationConnector)
  }

  "fetchByCollaboratorEmail" should {
    "return all the applications of the user" in new Setup {
      given(applicationConnector.fetchByCollaboratorEmail(collaboratorEmail)).willReturn(successful(Seq(application)))

      val result = await(underTest.fetchByCollaboratorEmail(collaboratorEmail))

      result shouldBe Seq(application)
    }
  }
}
