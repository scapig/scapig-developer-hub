package services

import connectors.ApiDefinitionConnector
import models._
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import utils.UnitSpec

import scala.concurrent.Future.{failed, successful}

class ApiDefinitionServiceSpec extends UnitSpec with MockitoSugar {

  val api = APIDefinition("apiName", "apiDescription", "apiContext",
    Seq(APIVersion("v1", APIStatus.PUBLISHED, Seq(Endpoint("/hello", "endpointDescription", HttpMethod.GET, AuthType.NONE)))))

  trait Setup {
    val apiDefinitionConnector = mock[ApiDefinitionConnector]

    val underTest = new ApiDefinitionService(apiDefinitionConnector)
  }

  "fetchAllApis" should {
    "return the API definitions" in new Setup {
      given(apiDefinitionConnector.fetchAllApis()).willReturn(successful(Seq(api)))

      val result = await(underTest.fetchAllApis())

      result shouldBe Seq(api)
    }
  }

  "fetchApi" should {
    "return the API definition" in new Setup {
      given(apiDefinitionConnector.fetchApi("aContext")).willReturn(successful(api))

      val result = await(underTest.fetchApi("aContext"))

      result shouldBe api
    }

    "propagate APINotFoundException when thrown by the connector" in new Setup {
      given(apiDefinitionConnector.fetchApi("aContext")).willReturn(failed(ApiNotFoundException()))

      intercept[ApiNotFoundException]{await(underTest.fetchApi("aContext"))}
    }

  }

}
