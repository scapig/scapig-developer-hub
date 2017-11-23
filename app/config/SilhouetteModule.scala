package config

import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api.util.ExtractableRequest
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import models.{Developer, SessionEnv}
import play.api.inject
import play.api.mvc.{RequestHeader, Result, Session}

import scala.concurrent.ExecutionContext.Implicits.global

class SilhouetteModule extends AbstractModule {


  @Provides
  def provideEnvironment(
                          identityService: IdentityService[Developer],
                          authenticatorService: AuthenticatorService[SessionAuthenticator],
                          eventBus: EventBus): Environment[SessionEnv] = {

    Environment[SessionEnv](
      identityService,
      authenticatorService,
      Seq(),                 // Here the request providers are set
      eventBus
    )
  }

  /**
    * Configures the module.
    */
  def configure() {
    inject.bind[AuthenticatorService[SessionAuthenticator]].to[AuthService]
    inject.bind[Silhouette[SessionEnv]].to[SilhouetteProvider[SessionEnv]]
  }
}

@Singleton
class AuthService extends AuthenticatorService[SessionAuthenticator] {
  override def create(loginInfo: LoginInfo)(implicit request: RequestHeader) = ???

  override def retrieve[B](implicit request: ExtractableRequest[B]) = ???

  override def init(authenticator: SessionAuthenticator)(implicit request: RequestHeader) = ???

  override def embed(value: Session, result: Result)(implicit request: RequestHeader) = ???

  override def embed(value: Session, request: RequestHeader) = ???

  override def touch(authenticator: SessionAuthenticator) = ???

  override def update(authenticator: SessionAuthenticator, result: Result)(implicit request: RequestHeader) = ???

  override def renew(authenticator: SessionAuthenticator)(implicit request: RequestHeader) = ???

  override def renew(authenticator: SessionAuthenticator, result: Result)(implicit request: RequestHeader) = ???

  override def discard(authenticator: SessionAuthenticator, result: Result)(implicit request: RequestHeader) = ???

  override implicit val executionContext = scala.concurrent.ExecutionContext.global
}

@Singleton
class IdService extends IdentityService[Developer] {
  override def retrieve(loginInfo: LoginInfo) = ???
}
