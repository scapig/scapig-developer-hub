package config

import java.util.UUID
import javax.inject.Inject

import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.actions.{SecuredErrorHandler, UnsecuredErrorHandler}
import com.mohiva.play.silhouette.api.crypto._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.crypto.{JcaCrypter, JcaCrypterSettings, JcaSigner, JcaSignerSettings}
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth1._
import com.mohiva.play.silhouette.impl.providers.oauth1.secrets.{CookieSecretProvider, CookieSecretSettings}
import com.mohiva.play.silhouette.impl.providers.oauth1.services.PlayOAuth1Service
import com.mohiva.play.silhouette.impl.providers.oauth2._
import com.mohiva.play.silhouette.impl.providers.openid.YahooProvider
import com.mohiva.play.silhouette.impl.providers.openid.services.PlayOpenIDService
import com.mohiva.play.silhouette.impl.providers.state.{CsrfStateItemHandler, CsrfStateSettings}
import com.mohiva.play.silhouette.impl.services._
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.password.{BCryptPasswordHasher, BCryptSha256PasswordHasher}
import com.mohiva.play.silhouette.persistence.daos.{DelegableAuthInfoDAO, InMemoryAuthInfoDAO}
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import play.api.Configuration
import play.api.libs.openid.OpenIdClient
import play.api.libs.ws.WSClient
import play.api.mvc.{CookieHeaderEncoding, RequestHeader, Results}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
 * The Guice module which wires all Silhouette dependencies.
 */
class SilhouetteModule extends AbstractModule {

  /**
   * Configures the module.
   */
  def configure() {
    bind(new TypeLiteral[Silhouette[DefaultEnv]]{}).to(new TypeLiteral[SilhouetteProvider[DefaultEnv]]{})
    bind(classOf[UnsecuredErrorHandler]).to(classOf[CustomUnsecuredErrorHandler])
    bind(classOf[SecuredErrorHandler]).to(classOf[CustomSecuredErrorHandler])

    bind(classOf[UserService]).to(classOf[UserServiceImpl])
    bind(classOf[CacheLayer]).to(classOf[PlayCacheLayer])
    bind(classOf[IDGenerator]).toInstance(new SecureRandomIDGenerator())
    bind(classOf[FingerprintGenerator]).toInstance(new DefaultFingerprintGenerator(false))
    bind(classOf[EventBus]).toInstance(EventBus())
    bind(classOf[Clock]).toInstance(Clock())

    // Replace this with the bindings to your concrete DAOs
//    bind(new TypeLiteral[DelegableAuthInfoDAO[PasswordInfo]]{}).to(new TypeLiteral[InMemoryAuthInfoDAO[PasswordInfo]]{})
//    bind(new TypeLiteral[DelegableAuthInfoDAO[OAuth1Info]]{}).to(new TypeLiteral[InMemoryAuthInfoDAO[OAuth1Info]]{})
//    bind(new TypeLiteral[DelegableAuthInfoDAO[OAuth2Info]]{}).to(new TypeLiteral[InMemoryAuthInfoDAO[OAuth2Info]]{})
//    bind(new TypeLiteral[DelegableAuthInfoDAO[OpenIDInfo]]{}).to(new TypeLiteral[InMemoryAuthInfoDAO[OpenIDInfo]]{})

    //    bind(classOf[DelegableAuthInfoDAO[PasswordInfo]]).toInstance(new InMemoryAuthInfoDAO[PasswordInfo])
//    bind(classOf[DelegableAuthInfoDAO[OAuth1Info]]).toInstance(new InMemoryAuthInfoDAO[OAuth1Info])
//    bind(classOf[DelegableAuthInfoDAO[OAuth2Info]]).toInstance(new InMemoryAuthInfoDAO[OAuth2Info])
//    bind(classOf[DelegableAuthInfoDAO[OpenIDInfo]]).toInstance(new InMemoryAuthInfoDAO[OpenIDInfo])
  }

  /**
   * Provides the HTTP layer implementation.
   *
   * @param client Play's WS client.
   * @return The HTTP layer implementation.
   */
  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  /**
   * Provides the Silhouette environment.
   *
   * @param userService The user service implementation.
   * @param authenticatorService The authentication service implementation.
   * @param eventBus The event bus instance.
   * @return The Silhouette environment.
   */
  @Provides
  def provideEnvironment(
    userService: UserService,
    authenticatorService: AuthenticatorService[CookieAuthenticator],
    eventBus: EventBus): Environment[DefaultEnv] = {

    Environment[DefaultEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  /**
   * Provides the social provider registry.
   *
   * @param facebookProvider The Facebook provider implementation.
   * @param googleProvider The Google provider implementation.
   * @param vkProvider The VK provider implementation.
   * @param twitterProvider The Twitter provider implementation.
   * @param xingProvider The Xing provider implementation.
   * @param yahooProvider The Yahoo provider implementation.
   * @return The Silhouette environment.
   */
  @Provides
  def provideSocialProviderRegistry(
    facebookProvider: FacebookProvider,
    googleProvider: GoogleProvider,
    vkProvider: VKProvider,
    twitterProvider: TwitterProvider,
    xingProvider: XingProvider,
    yahooProvider: YahooProvider): SocialProviderRegistry = {

    SocialProviderRegistry(Seq(
      googleProvider,
      facebookProvider,
      twitterProvider,
      vkProvider,
      xingProvider,
      yahooProvider
    ))
  }

  /**
   * Provides the signer for the OAuth1 token secret provider.
   *
   * @param configuration The Play configuration.
   * @return The signer for the OAuth1 token secret provider.
   */
  @Provides @Named("oauth1-token-secret-signer")
  def provideOAuth1TokenSecretSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.getObject("silhouette.oauth1TokenSecretProvider.signer").asInstanceOf[JcaSignerSettings]

    new JcaSigner(config)
  }

  /**
   * Provides the crypter for the OAuth1 token secret provider.
   *
   * @param configuration The Play configuration.
   * @return The crypter for the OAuth1 token secret provider.
   */
  @Provides @Named("oauth1-token-secret-crypter")
  def provideOAuth1TokenSecretCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.getObject("silhouette.oauth1TokenSecretProvider.crypter").asInstanceOf[JcaCrypterSettings]

    new JcaCrypter(config)
  }

  /**
   * Provides the signer for the CSRF state item handler.
   *
   * @param configuration The Play configuration.
   * @return The signer for the CSRF state item handler.
   */
  @Provides @Named("csrf-state-item-signer")
  def provideCSRFStateItemSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.getObject("silhouette.csrfStateItemHandler.signer").asInstanceOf[JcaSignerSettings]

    new JcaSigner(config)
  }

  /**
   * Provides the signer for the social state handler.
   *
   * @param configuration The Play configuration.
   * @return The signer for the social state handler.
   */
  @Provides @Named("social-state-signer")
  def provideSocialStateSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.getObject("silhouette.socialStateHandler.signer").asInstanceOf[JcaSignerSettings]

    new JcaSigner(config)
  }

  /**
   * Provides the signer for the authenticator.
   *
   * @param configuration The Play configuration.
   * @return The signer for the authenticator.
   */
  @Provides @Named("authenticator-signer")
  def provideAuthenticatorSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.getObject("silhouette.authenticator.signer").asInstanceOf[JcaSignerSettings]

    new JcaSigner(config)
  }

  /**
   * Provides the crypter for the authenticator.
   *
   * @param configuration The Play configuration.
   * @return The crypter for the authenticator.
   */
  @Provides @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.getObject("silhouette.authenticator.crypter").asInstanceOf[JcaCrypterSettings]

    new JcaCrypter(config)
  }

  /**
   * Provides the auth info repository.
   *
   * @param passwordInfoDAO The implementation of the delegable password auth info DAO.
   * @param oauth1InfoDAO The implementation of the delegable OAuth1 auth info DAO.
   * @param oauth2InfoDAO The implementation of the delegable OAuth2 auth info DAO.
   * @param openIDInfoDAO The implementation of the delegable OpenID auth info DAO.
   * @return The auth info repository instance.
   */
  @Provides
  def provideAuthInfoRepository(
//    passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo],
//    oauth1InfoDAO: DelegableAuthInfoDAO[OAuth1Info],
//    oauth2InfoDAO: DelegableAuthInfoDAO[OAuth2Info],
//    openIDInfoDAO: DelegableAuthInfoDAO[OpenIDInfo]
                               ): AuthInfoRepository = {

//    new DelegableAuthInfoRepository(passwordInfoDAO, oauth1InfoDAO, oauth2InfoDAO, openIDInfoDAO)
    new DelegableAuthInfoRepository()
  }

  /**
   * Provides the authenticator service.
   *
   * @param signer The signer implementation.
   * @param crypter The crypter implementation.
   * @param cookieHeaderEncoding Logic for encoding and decoding `Cookie` and `Set-Cookie` headers.
   * @param fingerprintGenerator The fingerprint generator implementation.
   * @param idGenerator The ID generator implementation.
   * @param configuration The Play configuration.
   * @param clock The clock instance.
   * @return The authenticator service.
   */
  @Provides
  def provideAuthenticatorService(
    @Named("authenticator-signer") signer: Signer,
    @Named("authenticator-crypter") crypter: Crypter,
    cookieHeaderEncoding: CookieHeaderEncoding,
    fingerprintGenerator: FingerprintGenerator,
    idGenerator: IDGenerator,
    configuration: Configuration,
    clock: Clock): AuthenticatorService[CookieAuthenticator] = {

    val config = configuration.underlying.getObject("silhouette.authenticator").asInstanceOf[CookieAuthenticatorSettings]
    val authenticatorEncoder = new CrypterAuthenticatorEncoder(crypter)

    new CookieAuthenticatorService(config, None, signer, cookieHeaderEncoding, authenticatorEncoder, fingerprintGenerator, idGenerator, clock)
  }

  /**
   * Provides the avatar service.
   *
   * @param httpLayer The HTTP layer implementation.
   * @return The avatar service implementation.
   */
  @Provides
  def provideAvatarService(httpLayer: HTTPLayer): AvatarService = new GravatarService(httpLayer)

  /**
   * Provides the OAuth1 token secret provider.
   *
   * @param signer The signer implementation.
   * @param crypter The crypter implementation.
   * @param configuration The Play configuration.
   * @param clock The clock instance.
   * @return The OAuth1 token secret provider implementation.
   */
  @Provides
  def provideOAuth1TokenSecretProvider(
    @Named("oauth1-token-secret-signer") signer: Signer,
    @Named("oauth1-token-secret-crypter") crypter: Crypter,
    configuration: Configuration,
    clock: Clock): OAuth1TokenSecretProvider = {

    val settings = configuration.underlying.getObject("silhouette.oauth1TokenSecretProvider").asInstanceOf[CookieSecretSettings]
    new CookieSecretProvider(settings, signer, crypter, clock)
  }

  /**
   * Provides the CSRF state item handler.
   *
   * @param idGenerator The ID generator implementation.
   * @param signer The signer implementation.
   * @param configuration The Play configuration.
   * @return The CSRF state item implementation.
   */
  @Provides
  def provideCsrfStateItemHandler(
    idGenerator: IDGenerator,
    @Named("csrf-state-item-signer") signer: Signer,
    configuration: Configuration): CsrfStateItemHandler = {
    val settings = configuration.underlying.getObject("silhouette.csrfStateItemHandler").asInstanceOf[CsrfStateSettings]
    new CsrfStateItemHandler(settings, idGenerator, signer)
  }

  /**
   * Provides the social state handler.
   *
   * @param signer The signer implementation.
   * @return The social state handler implementation.
   */
  @Provides
  def provideSocialStateHandler(
    @Named("social-state-signer") signer: Signer,
    csrfStateItemHandler: CsrfStateItemHandler): SocialStateHandler = {

    new DefaultSocialStateHandler(Set(csrfStateItemHandler), signer)
  }

  /**
   * Provides the password hasher registry.
   *
   * @return The password hasher registry.
   */
  @Provides
  def providePasswordHasherRegistry(): PasswordHasherRegistry = {
    PasswordHasherRegistry(new BCryptSha256PasswordHasher(), Seq(new BCryptPasswordHasher()))
  }

  /**
   * Provides the credentials provider.
   *
   * @param authInfoRepository The auth info repository implementation.
   * @param passwordHasherRegistry The password hasher registry.
   * @return The credentials provider.
   */
  @Provides
  def provideCredentialsProvider(
    authInfoRepository: AuthInfoRepository,
    passwordHasherRegistry: PasswordHasherRegistry): CredentialsProvider = {

    new CredentialsProvider(authInfoRepository, passwordHasherRegistry)
  }

  /**
   * Provides the Facebook provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param socialStateHandler The social state handler implementation.
   * @param configuration The Play configuration.
   * @return The Facebook provider.
   */
  @Provides
  def provideFacebookProvider(
    httpLayer: HTTPLayer,
    socialStateHandler: SocialStateHandler,
    configuration: Configuration): FacebookProvider = {

    new FacebookProvider(httpLayer, socialStateHandler, configuration.underlying.getObject("silhouette.facebook").asInstanceOf[OAuth2Settings])
  }

  /**
   * Provides the Google provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param socialStateHandler The social state handler implementation.
   * @param configuration The Play configuration.
   * @return The Google provider.
   */
  @Provides
  def provideGoogleProvider(
    httpLayer: HTTPLayer,
    socialStateHandler: SocialStateHandler,
    configuration: Configuration): GoogleProvider = {

    new GoogleProvider(httpLayer, socialStateHandler, configuration.underlying.getObject("silhouette.google").asInstanceOf[OAuth2Settings])
  }

  /**
   * Provides the VK provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param socialStateHandler The social state handler implementation.
   * @param configuration The Play configuration.
   * @return The VK provider.
   */
  @Provides
  def provideVKProvider(
    httpLayer: HTTPLayer,
    socialStateHandler: SocialStateHandler,
    configuration: Configuration): VKProvider = {

    new VKProvider(httpLayer, socialStateHandler, configuration.underlying.getObject("silhouette.vk").asInstanceOf[OAuth2Settings])
  }

  /**
   * Provides the Twitter provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param tokenSecretProvider The token secret provider implementation.
   * @param configuration The Play configuration.
   * @return The Twitter provider.
   */
  @Provides
  def provideTwitterProvider(
    httpLayer: HTTPLayer,
    tokenSecretProvider: OAuth1TokenSecretProvider,
    configuration: Configuration): TwitterProvider = {

    val settings = configuration.underlying.getObject("silhouette.twitter").asInstanceOf[OAuth1Settings]
    new TwitterProvider(httpLayer, new PlayOAuth1Service(settings), tokenSecretProvider, settings)
  }

  /**
   * Provides the Xing provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param tokenSecretProvider The token secret provider implementation.
   * @param configuration The Play configuration.
   * @return The Xing provider.
   */
  @Provides
  def provideXingProvider(
    httpLayer: HTTPLayer,
    tokenSecretProvider: OAuth1TokenSecretProvider,
    configuration: Configuration): XingProvider = {

    val settings = configuration.underlying.getObject("silhouette.xing").asInstanceOf[OAuth1Settings]
    new XingProvider(httpLayer, new PlayOAuth1Service(settings), tokenSecretProvider, settings)
  }

  /**
   * Provides the Yahoo provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param client The OpenID client implementation.
   * @param configuration The Play configuration.
   * @return The Yahoo provider.
   */
  @Provides
  def provideYahooProvider(
    httpLayer: HTTPLayer,
    client: OpenIdClient,
    configuration: Configuration): YahooProvider = {

    val settings = configuration.underlying.getObject("silhouette.yahoo").asInstanceOf[OpenIDSettings]
    new YahooProvider(httpLayer, new PlayOpenIDService(client, settings), settings)
  }
}


trait DefaultEnv extends Env {
  type I = User
  type A = CookieAuthenticator
}

case class User(
                 userID: UUID) extends Identity

class CustomUnsecuredErrorHandler extends UnsecuredErrorHandler {
  override def onNotAuthorized(implicit request: RequestHeader) = {
    Future.successful(Results.Ok("THV"))
  }
}

class CustomSecuredErrorHandler @Inject()() extends SecuredErrorHandler {
  override def onNotAuthenticated(implicit request: RequestHeader) = {
    Future.successful(Results.Ok("THV"))
  }

  override def onNotAuthorized(implicit request: RequestHeader) = {
    Future.successful(Results.Ok("THV"))
  }
}

trait UserService extends IdentityService[User] {

}

class UserServiceImpl @Inject()()(implicit ex: ExecutionContext) extends UserService {
  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = Future.successful(None)
}