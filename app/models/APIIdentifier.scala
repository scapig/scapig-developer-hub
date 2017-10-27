package models

case class APIIdentifier(context: String, version: String)

case class APIDefinition(name: String, context: String, versions: Seq[APIVersion])

case class APIVersion(
                       version: String,
                       status: APIStatus.Value)

object APIStatus extends Enumeration {
  type APIStatus = Value
  val PROTOTYPED, PUBLISHED, DEPRECATED, RETIRED = Value
}
