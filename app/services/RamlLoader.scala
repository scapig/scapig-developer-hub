package services

import models.{RamlNotFoundException, RamlParseException, RamlUnsupportedVersionException}
import org.raml.v2.api.loader._
import org.raml.v2.api.{RamlModelBuilder, RamlModelResult}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

trait RamlLoader {

  private val RamlDoesNotExist = "Raml does not exist at:"
  private val unsupportedSpecVersion: Try[RAML] = Failure(RamlUnsupportedVersionException("Only RAML1.0 is supported"))

  def load(resource: String): Try[RAML]

  protected def verify(result: RamlModelResult): Try[RAML] = {
    result.getValidationResults.toSeq match {
      case Nil => Option(result.getApiV10).fold(unsupportedSpecVersion) { api => Success(api) }
      case errors => {
        val msg = errors.map(e => transformError(e.toString)).mkString("; ")
        if (msg.contains(RamlDoesNotExist)) Failure(RamlNotFoundException(msg))
        else Failure(RamlParseException(msg))
      }
    }
  }

  protected def transformError(msg: String) = msg
}

class ClasspathRamlLoader extends RamlLoader {
  override def load(classpath: String) = {
    val builder = new RamlModelBuilder(new CompositeResourceLoader(
      new ClassPathResourceLoader(),
      new UrlResourceLoader()
    ))

    verify(builder.buildApi(classpath))
  }
}

class StringRamlLoader extends RamlLoader {
  override def load(content: String) = {
    val builder = new RamlModelBuilder()
    val api = builder.buildApi(content, "")
    verify(api)
  }
}

