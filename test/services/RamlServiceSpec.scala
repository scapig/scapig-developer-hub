package services

import connectors.PublisherConnector
import models.{ApiNotFoundException, RamlParseException}
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import utils.UnitSpec

import scala.concurrent.Future.successful
import scala.util.{Failure, Success}

class RamlServiceSpec extends UnitSpec with MockitoSugar {

  val ramlContent = "RAML CONTENT"
  val raml = new ClasspathRamlLoader().load("calendar.raml").get

  val apiContext = "calendar"
  val apiVersion = "2.0"

  trait Setup {
    val publisherConnector = mock[PublisherConnector]
    val ramlLoader = mock[StringRamlLoader]

    val underTest = new RamlService(publisherConnector, ramlLoader)
  }

  "fetchRaml" should {
    "return the RAML" in new Setup {
      given(publisherConnector.fetchRaml(apiContext, apiVersion)).willReturn(successful(Some(ramlContent)))
      given(ramlLoader.load(ramlContent)).willReturn(Success(raml))

      val result = await(underTest.fetchRaml(apiContext, apiVersion))

      result shouldBe raml
    }

    "fail with APINotFoundException when the RAML can not be found" in new Setup {
      given(publisherConnector.fetchRaml(apiContext, apiVersion)).willReturn(successful(None))

      intercept[ApiNotFoundException]{await(underTest.fetchRaml(apiContext, apiVersion))}
    }

    "fail with APINotFoundException when the RAML can not be loaded" in new Setup {
      given(publisherConnector.fetchRaml(apiContext, apiVersion)).willReturn(successful(Some(ramlContent)))
      given(ramlLoader.load(ramlContent)).willReturn(Failure(RamlParseException("test error")))

      intercept[ApiNotFoundException]{await(underTest.fetchRaml(apiContext, apiVersion))}
    }

  }
}
