package models

import org.joda.time.DateTime
import play.api.libs.json.{JodaReads, JodaWrites, _}

object JsonFormatters {
  val datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  implicit val dateRead: Reads[DateTime] = JodaReads.jodaDateReads(datePattern)
  implicit val dateWrite: Writes[DateTime] = JodaWrites.jodaDateWrites(datePattern)
  implicit val dateFormat: Format[DateTime] = Format[DateTime](dateRead, dateWrite)

  implicit val formatRole = EnumJson.enumFormat(Role)
  implicit val formatRateLimitTier = EnumJson.enumFormat(RateLimitTier)
  implicit val formatAPIStatus = EnumJson.enumFormat(APIStatus)

  implicit val formatClientSecret = Json.format[ClientSecret]
  implicit val formatEnvironmentCredentials = Json.format[EnvironmentCredentials]
  implicit val formatApplicationCredentials = Json.format[ApplicationCredentials]
  implicit val formatApplicationUrls = Json.format[ApplicationUrls]
  implicit val formatCollaborator = Json.format[Collaborator]
  implicit val formatApplication = Json.format[Application]

  implicit val formatCreateApplicationRequest = Json.format[CreateApplicationRequest]

  implicit val formatDeveloper = Json.format[Developer]
  implicit val formatUserCreateRequest = Json.format[UserCreateRequest]

  implicit val formatSession = Json.format[Session]
  implicit val formatSessionResponse = Json.format[SessionResponse]
  implicit val formatSessionCreateRequest = Json.format[SessionCreateRequest]

  implicit val formatAPIIdentifier = Json.format[APIIdentifier]
  implicit val formatAPIVersion = Json.format[APIVersion]
  implicit val formatAPIDefinition = Json.format[APIDefinition]
}

object EnumJson {

  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) => {
        try {
          JsSuccess(enum.withName(s))
        } catch {
          case _: NoSuchElementException =>
            JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not contain '$s'")
        }
      }
      case _ => JsError("String value expected")
    }
  }

  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

  implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
  }

}