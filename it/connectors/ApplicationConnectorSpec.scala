package connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import models._
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.Status
import utils.UnitSpec
import models.JsonFormatters._

class ApplicationConnectorSpec extends UnitSpec with BeforeAndAfterAll with BeforeAndAfterEach {
  val port = 7001

  val playApplication = new GuiceApplicationBuilder()
    .configure("services.application.host" -> "localhost")
    .configure("services.application.port" -> "7001")
    .build()
  val wireMockServer = new WireMockServer(wireMockConfig().port(port))

  val collaboratorEmail = "admin@app.com"
  val redirectUris = Seq("http://redirecturi")
  val prodCredentials = EnvironmentCredentials("prodClientId", "prodServerToken", Seq(ClientSecret("prodSecret", DateTime.now())))
  val sandboxCredentials = EnvironmentCredentials("sandboxClientId", "sandboxServerToken", Seq(ClientSecret("sandboxSecret", DateTime.now())))
  val application = Application("app name", "app description", Set(Collaborator(collaboratorEmail, Role.ADMINISTRATOR)), redirectUris,
    ApplicationCredentials(prodCredentials, sandboxCredentials), DateTime.now(), RateLimitTier.BRONZE)

  override def beforeAll {
    configureFor(port)
    wireMockServer.start()
  }

  override def afterAll: Unit = {
    wireMockServer.stop()
  }

  override def beforeEach(): Unit = {
    WireMock.reset()
  }

  trait Setup {
    val applicationConnector = playApplication.injector.instanceOf[ApplicationConnector]
  }

  "fetchByCollaboratorEmail" should {
    "return the user's applications" in new Setup {
      stubFor(get(s"/applications?collaboratorEmail=$collaboratorEmail").willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody(Json.toJson(Seq(application)).toString())))

      val result = await(applicationConnector.fetchByCollaboratorEmail(collaboratorEmail))

      result shouldBe Seq(application)
    }
  }

  "create" should {
    "create an application and return it in the response" in new Setup {
      val request = CreateApplicationRequest(application.name, application.description, application.redirectUris, application.collaborators)

      stubFor(post(s"/application").withRequestBody(equalToJson(Json.toJson(request).toString())).willReturn(aResponse()
        .withStatus(Status.CREATED)
        .withBody(Json.toJson(application).toString())))

      val result = await(applicationConnector.create(request))

      result shouldBe application
    }
  }

  "update" should {
    "update an application and return it in the response" in new Setup {
      val request = UpdateApplicationRequest(application.name, application.description, application.redirectUris, application.rateLimitTier)
      val updatedApplication = application.copy(name = "updatedName")

      stubFor(post(s"/application/${application.id}").withRequestBody(equalToJson(Json.toJson(request).toString())).willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody(Json.toJson(updatedApplication).toString())))

      val result = await(applicationConnector.update(application.id.toString, request))

      result shouldBe updatedApplication
    }
  }

  "fetch" should {
    "return the application" in new Setup {
      stubFor(get(s"/application/${application.id}").willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody(Json.toJson(application).toString())))

      val result = await(applicationConnector.fetch(application.id.toString))

      result shouldBe application
    }

    "fail with ApplicationNotFound when the application does not exist" in new Setup {
      stubFor(get(s"/application/notexist").willReturn(aResponse()
        .withStatus(Status.NOT_FOUND)))

      intercept[ApplicationNotFoundException] {
        await(applicationConnector.fetch("notexist"))
      }
    }
  }

  "fetchSubscriptions" should {
    "return the application subscriptions" in new Setup {
      val apiIdentifier = APIIdentifier("context", "version")

      stubFor(get(s"/application/${application.id}/subscription").willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody(Json.toJson(Seq(apiIdentifier)).toString())))

      val result = await(applicationConnector.fetchSubscriptions(application.id.toString))

      result shouldBe Seq(apiIdentifier)
    }

    "fail with ApplicationNotFound when the application does not exist" in new Setup {
      stubFor(get(s"/application/${application.id}/subscription").willReturn(aResponse()
        .withStatus(Status.NOT_FOUND)))

      intercept[ApplicationNotFoundException] {
        await(applicationConnector.fetchSubscriptions(application.id.toString))
      }
    }

  }

  "subscribeToApi" should {
    val api = APIIdentifier("aContext", "aVersion")

    "subscribe the application to the API" in new Setup {
      stubFor(post(s"/application/${application.id}/subscription?context=aContext&version=aVersion").willReturn(aResponse()
        .withStatus(Status.NO_CONTENT)))

      val result = await(applicationConnector.subscribeToApi(application.id.toString, api))

      result shouldBe HasSucceeded
    }

    "fail with ApplicationNotFound when the application does not exist" in new Setup {
      stubFor(post(s"/application/${application.id}/subscription?context=aContext&version=aVersion").willReturn(aResponse()
        .withStatus(Status.NOT_FOUND)))

      intercept[ApplicationNotFoundException] {
        await(applicationConnector.subscribeToApi(application.id.toString, api))
      }
    }
  }

  "unsubscribeToApi" should {
    val api = APIIdentifier("aContext", "aVersion")

    "unsubscribe the application to the API" in new Setup {
      stubFor(delete(s"/application/${application.id}/subscription?context=aContext&version=aVersion").willReturn(aResponse()
        .withStatus(Status.NO_CONTENT)))

      val result = await(applicationConnector.unsubscribeToApi(application.id.toString, api))

      result shouldBe HasSucceeded
    }

    "fail with ApplicationNotFound when the application does not exist" in new Setup {
      stubFor(delete(s"/application/${application.id}/subscription?context=aContext&version=aVersion").willReturn(aResponse()
        .withStatus(Status.NOT_FOUND)))

      intercept[ApplicationNotFoundException] {
        await(applicationConnector.unsubscribeToApi(application.id.toString, api))
      }
    }

  }

}