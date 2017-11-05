package connectors

import java.util.UUID

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import models.JsonFormatters._
import models.{UserCreateRequest, _}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.Status
import utils.UnitSpec

class DeveloperConnectorSpec extends UnitSpec with BeforeAndAfterAll with BeforeAndAfterEach {
  val port = 7001

  val playApplication = new GuiceApplicationBuilder()
    .configure("services.tapi-developer.port" -> "7001")
    .build()
  val wireMockServer = new WireMockServer(wireMockConfig().port(port))

  val userCreateRequest = UserCreateRequest("user@test.com", "aPassword", "John", "Doe")
  val developer = Developer("user@test.com", "John", "Doe")
  val sessionId = UUID.randomUUID().toString
  val sessionResponse = SessionResponse(Session("user@test.com", sessionId), developer)

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
    val developerConnector = playApplication.injector.instanceOf[DeveloperConnector]
  }

  "register" should {
    "return the user when it is created successfully" in new Setup {
      stubFor(post(s"/developer").withRequestBody(equalToJson(Json.toJson(userCreateRequest).toString())).willReturn(aResponse()
        .withStatus(Status.CREATED)
        .withBody(Json.toJson(developer).toString())))

      val result = await(developerConnector.register(userCreateRequest))

      result shouldBe developer
    }

    "fail with UserAlreadyRegisteredException when the user already exists" in new Setup {
      stubFor(post(s"/developer").withRequestBody(equalToJson(Json.toJson(userCreateRequest).toString())).willReturn(aResponse()
        .withStatus(Status.CONFLICT)))

      intercept[UserAlreadyRegisteredException]{await(developerConnector.register(userCreateRequest))}
    }

  }

  "createSession" should {
    val sessionCreateRequest = SessionCreateRequest("user@test.com", "aPassword")

    "return the session when the credentials are valid" in new Setup {
      stubFor(post(s"/session").withRequestBody(equalToJson(Json.toJson(sessionCreateRequest).toString())).willReturn(aResponse()
        .withStatus(Status.CREATED)
        .withBody(Json.toJson(sessionResponse).toString())))

      val result = await(developerConnector.createSession(sessionCreateRequest))

      result shouldBe sessionResponse
    }

    "fail with InvalidCredentialsException when the credentials are invalid" in new Setup {
      stubFor(post(s"/session").withRequestBody(equalToJson(Json.toJson(sessionCreateRequest).toString())).willReturn(aResponse()
        .withStatus(Status.UNAUTHORIZED)))

      intercept[InvalidCredentialsException]{await(developerConnector.createSession(sessionCreateRequest))}
    }
  }

  "fetchSession" should {
    "return the session" in new Setup {
      stubFor(get(s"/session/$sessionId").willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody(Json.toJson(sessionResponse).toString())))

      val result = await(developerConnector.fetchSession(sessionId))

      result shouldBe sessionResponse
    }

  }

  "fetchDeveloper" should {
    "return the developer" in new Setup {
      stubFor(get(urlPathEqualTo("/developer")).withQueryParam("email", equalTo(developer.email))
        .willReturn(aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(developer).toString())))

      val result = await(developerConnector.fetchDeveloper(developer.email))

      result shouldBe developer
    }

  }

  "updateProfile" should {
    val userProfileEditRequest = UserProfileEditRequest("newFirstName", "newLastName")
    val updatedDeveloper = developer.copy(firstName = "newFirstName", lastName = "newLastName")

    "update the user profile" in new Setup {
      stubFor(post(urlPathEqualTo(s"/developer/${developer.email}")).withRequestBody(equalToJson(Json.toJson(userProfileEditRequest).toString()))
        .willReturn(aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(updatedDeveloper).toString())))

      val result = await(developerConnector.updateProfile(developer.email, userProfileEditRequest))

      result shouldBe HasSucceeded
    }
  }

  "changePassword" should {
    val changePasswordRequest = ChangePasswordRequest("oldPassword", "newPassword")

    "changePassword" in new Setup {
      stubFor(post(urlPathEqualTo(s"/developer/${developer.email}/password")).withRequestBody(equalToJson(Json.toJson(changePasswordRequest).toString()))
        .willReturn(aResponse()
          .withStatus(Status.OK)))

      val result = await(developerConnector.changePassword(developer.email, changePasswordRequest))

      result shouldBe HasSucceeded
    }

    "fail with InvalidCredentialsException when the password is invalid" in new Setup {
      stubFor(post(urlPathEqualTo(s"/developer/${developer.email}/password")).withRequestBody(equalToJson(Json.toJson(changePasswordRequest).toString()))
        .willReturn(aResponse()
          .withStatus(Status.UNAUTHORIZED)))

      intercept[InvalidCredentialsException]{await(developerConnector.changePassword(developer.email, changePasswordRequest))}
    }

  }

  "deleteSession" should {
    "delete the session" in new Setup {
      stubFor(delete(s"/session/$sessionId").willReturn(aResponse()
        .withStatus(Status.NO_CONTENT)))

      val result = await(developerConnector.deleteSession(sessionId))

      result shouldBe HasSucceeded
    }

  }

}