package models


case class ApplicationViewData(app: Application, subscriptions: Seq[APISubscription])

case class APISubscription(apiName: String, context: String, versions: Seq[APIVersionSubscription])

case class APIVersionSubscription(version: APIVersion, subscribed: Boolean)

object APIVersionSubscription {
  def apply(apiContext: String, apiVersion: APIVersion, subscribedApis: Seq[APIIdentifier]): APIVersionSubscription = {
    APIVersionSubscription(apiVersion, subscribedApis.exists(s => s.context == apiContext && s.version == apiVersion.version))
  }
}