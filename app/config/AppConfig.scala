package config

import javax.inject.Inject

import models.RateLimitTier.RateLimitTier
import models.RateLimitTier
import play.api.Configuration

class AppConfig @Inject()(configuration: Configuration) {
  lazy val homepage = configuration.getOptional[String]("homepage")
  lazy val sessionTimeout = configuration.get[Int]("session.timeout")
  lazy val apiDocumentationFrontendUrl = homepage.getOrElse(serviceUrl("api-documentation-frontend"))
  lazy val gatewayBaseUrl = configuration.get[String]("gatewayBaseUrl")

  lazy val rateLimitGold: Int =  configuration.get[Int]("rateLimit.gold")
  lazy val rateLimitSilver: Int =  configuration.get[Int]("rateLimit.silver")
  lazy val rateLimitBronze: Int =  configuration.get[Int]("rateLimit.bronze")

  def requestsPerMinute(rateLimitTier: RateLimitTier): Int = {
    rateLimitTier match {
      case RateLimitTier.GOLD => rateLimitGold
      case RateLimitTier.SILVER => rateLimitSilver
      case _ => rateLimitBronze
    }
  }

  def serviceUrl(serviceName: String): String = {
    val method = configuration.getOptional[String](s"services.$serviceName.method").getOrElse("http")
    val host = configuration.get[String](s"services.$serviceName.host")
    val port = configuration.get[String](s"services.$serviceName.port")
    s"$method://$host:$port"
  }
}
