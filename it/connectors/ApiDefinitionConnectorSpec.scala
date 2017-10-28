package connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import models.JsonFormatters._
import models._
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.Status
import utils.UnitSpec

class ApiDefinitionConnectorSpec extends UnitSpec with BeforeAndAfterAll with BeforeAndAfterEach {
  val port = 7001

  val playApplication = new GuiceApplicationBuilder()
    .configure("services.tapi-definition.port" -> "7001")
    .build()
  val wireMockServer = new WireMockServer(wireMockConfig().port(port))

  val api = APIDefinition("apiName", "apiContext", Seq(APIVersion("v1", APIStatus.PUBLISHED)))

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
    val apiDefinitionConnector = playApplication.injector.instanceOf[ApiDefinitionConnector]
  }

  "findAll" should {
    "return all the API Definition" in new Setup {
      stubFor(get(s"/apis").willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody(Json.toJson(Seq(api)).toString())))

      val result = await(apiDefinitionConnector.fetchAllApis())

      result shouldBe Seq(api)
    }
  }
}