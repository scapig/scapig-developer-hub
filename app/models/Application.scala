package models

import java.util.UUID

import models.Role.Role
import org.joda.time.DateTime

case class Application(name: String,
                       description: String,
                       collaborators: Set[Collaborator],
                       applicationUrls: ApplicationUrls,
                       credentials: ApplicationCredentials,
                       createdOn: DateTime,
                       rateLimitTier: RateLimitTier.Value,
                       id: UUID = UUID.randomUUID()) {

  def role(email: String): Option[Role] = collaborators.find(_.emailAddress == email).map(_.role)
}

case class Collaborator(emailAddress: String, role: Role.Value)

object Role extends Enumeration {
  type Role = Value
  val DEVELOPER, ADMINISTRATOR = Value
}

case class ApplicationUrls(redirectUris: Seq[String],
                           termsAndConditionsUrl: String,
                           privacyPolicyUrl: String)

case class ApplicationCredentials(production: EnvironmentCredentials,
                                  sandbox: EnvironmentCredentials)

case class EnvironmentCredentials(clientId: String,
                                  serverToken: String,
                                  clientSecrets: Seq[ClientSecret])

case class ClientSecret(secret: String,
                        createdOn: DateTime)

object RateLimitTier extends Enumeration {
  type RateLimitTier = Value
  val GOLD, SILVER, BRONZE = Value
}

case class CreateApplicationRequest(name: String,
                                    description: String,
                                    applicationUrls: ApplicationUrls,
                                    collaborators: Set[Collaborator])

case class ApplicationSummary(id: String, name: String, description: String, role: Role)

object ApplicationSummary {
  def apply(app: Application, email: String): ApplicationSummary = {
    ApplicationSummary(app.id.toString, app.name, app.description, app.role(email).getOrElse(throw new IllegalStateException("email should be a collaborator of the application")))
  }
}