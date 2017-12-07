package models

import models.AuthType.AuthType
import org.raml.v2.api.model.v10.bodies.Response
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration
import org.raml.v2.api.model.v10.methods.Method
import org.raml.v2.api.model.v10.resources.Resource
import services.RAML

import scala.collection.JavaConversions._

object ResourceUtils {

  def resources(resources: Seq[Resource]) = {
    flatten(resources).filterNot(_.methods().isEmpty)
  }

  private def flatten(resources: Seq[Resource], acc: Seq[Resource] = Nil): Seq[Resource] = {
    resources match {
      case head +: tail => flatten(tail, flatten(head.resources, acc :+ head))
      case _ => acc
    }
  }
}

case class Authorisation(authType: AuthType, scope: Option[String] = None)

object Authorisation {
  def apply(method: Method) : Authorisation = {
    method.securedBy().toList match {
      case Nil => Authorisation(AuthType.NONE)
      case head :: tail if head.securityScheme().`type`() == "OAuth 2.0" => Authorisation(AuthType.USER, getScope(method))
      case _ => Authorisation(AuthType.APPLICATION)
    }
  }

  private def getScope(method: Method): Option[String] = {
    method.securedBy()
      .find(_.name() == "oauth_2_0").flatMap(_.structuredValue().properties()
      .find(_.name() == "scopes").flatMap(_.values().headOption
      .map(_.value().toString)))
  }
}

object Response {
  def success(method: Method) = method.responses.filter(isSuccessResponse)

  def error(method: Method) = method.responses.filterNot(isSuccessResponse)

  private def isSuccessResponse(response: Response) = {
    response.code().value().startsWith("2") || response.code().value().startsWith("3")
  }

}
