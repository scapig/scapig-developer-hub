package models

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

case class APIIdentifier(context: String, version: String)

case class APIDefinition(name: String, description: String, context: String, versions: Seq[APIVersion]) {
  lazy val encodedContext = URLEncoder.encode(context, StandardCharsets.UTF_8.name())
}

case class APIVersion(
                       version: String,
                       status: APIStatus.Value,
                       endpoints: Seq[Endpoint])

case class Endpoint(
                     uriPattern: String,
                     endpointName: String,
                     method: HttpMethod.Value,
                     authType: AuthType.Value,
                     scope: Option[String] = None,
                     queryParameters: Seq[Parameter] = Seq.empty)

case class Parameter(name: String, required: Boolean = false)

object APIStatus extends Enumeration {
  type APIStatus = Value
  val PROTOTYPED, PUBLISHED, DEPRECATED, RETIRED = Value
}

object AuthType extends Enumeration {
  type AuthType = Value
  val NONE, APPLICATION, USER = Value
}

object HttpMethod extends Enumeration {
  type HttpMethod = Value
  val GET, POST, PUT, DELETE, OPTIONS = Value
}
