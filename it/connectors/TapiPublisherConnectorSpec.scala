package connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.inject.guice.GuiceApplicationBuilder
import play.mvc.Http.Status
import utils.UnitSpec

class TapiPublisherConnectorSpec extends UnitSpec with BeforeAndAfterAll with BeforeAndAfterEach {
  val port = 7001


  val playApplication = new GuiceApplicationBuilder()
    .configure("services.tapi-publisher.host" -> "localhost")
    .configure("services.tapi-publisher.port" -> "7001")
    .build()
  val wireMockServer = new WireMockServer(wireMockConfig().port(port))

  val ramlContent = "RAML CONTENT"

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
    val tapiPublisherConnector = playApplication.injector.instanceOf[TapiPublisherConnector]
  }

  "fetchRaml" should {
    "return the RAML when it exists" in new Setup {
      stubFor(get(urlPathEqualTo("/raml"))
        .withQueryParam("context", equalTo("aContext"))
        .withQueryParam("version", equalTo("aVersion")).willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody(ramlContent)))

      val result = await(tapiPublisherConnector.fetchRaml("aContext", "aVersion"))

      result shouldBe Some(ramlContent)
    }

    "return None when it doesnt exist" in new Setup {
      stubFor(get(urlPathEqualTo("/raml"))
        .withQueryParam("context", equalTo("aContext"))
        .withQueryParam("version", equalTo("aVersion")).willReturn(aResponse()
        .withStatus(Status.NOT_FOUND)))

      val result = await(tapiPublisherConnector.fetchRaml("aContext", "aVersion"))

      result shouldBe None
    }

  }
}
