package com.rockthejvm.jobsboard.modules

import cats.data.OptionT
import cats.implicits.*
import cats.effect.*
import org.http4s.HttpRoutes
import org.http4s.dsl.*
import org.http4s.server.*
import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import tsec.authentication.BackingStore
import tsec.common.SecureRandomId
import tsec.authentication.JWTAuthenticator
import tsec.authentication.SecuredRequestHandler
import org.typelevel.log4cats.Logger

import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.http.routes.{HealthRoutes, JobRoutes, AuthRoutes}
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.config.SecurityConfig

class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F], authenticator: Authenticator[F]) {

  given securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)
  private val healthRoutes                = HealthRoutes[F].routes
  private val jobRoutes                   = JobRoutes[F](core.jobs).routes
  private val authRoutes                  = AuthRoutes[F](core.auth, authenticator).routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> jobRoutes <+> authRoutes)
  )
}

object HttpApi {

  def createAuthenticator[F[_]: Sync](
      users: Users[F],
      securityConfig: SecurityConfig
  ): F[Authenticator[F]] = {
    //  1. Identity store to retrieve users: String => OptionT[F, User]
    //  basically map which returns user by their ID (String)
    val idStore: IdentityStore[F, String, User] = (email: String) => OptionT(users.find(email))

    //  2. backing store for JWT tokens: BackingStore[F, id, JwtToken]
    //  possibility to have mutable map -> race conditions
    //  r=> ef
    val tokenStoreF = Ref.of[F, Map[SecureRandomId, JwtToken]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JwtToken] {
        override def get(id: SecureRandomId): OptionT[F, JwtToken] =
          OptionT( /*F[JtToken*/ ref.get.map(_.get(id)))

        override def put(elem: JwtToken): F[JwtToken] =
          ref.modify(store => (store + (elem.id -> elem), elem))

        override def update(v: JwtToken): F[JwtToken] = put(v)
        override def delete(id: SecureRandomId): F[Unit] =
          ref.modify(store => (store - id, ()))
      }
    }

    //  3. hashing key
    val keyF =
      HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8")) //  TODO: move to config

    for {
      key        <- keyF
      tokenStore <- tokenStoreF
    } yield
    //  4. jwt authenticator
    JWTAuthenticator.backed.inBearerToken(
      expiryDuration = securityConfig.jwtExpiryDuration, //  expiry of tokens,
      maxIdle = None,                                    //  max idle time
      identityStore = idStore,                           //  identity store
      tokenStore = tokenStore,
      signingKey = key //  hash key
    )
  }

  def apply[F[_]: Async: Logger](
      core: Core[F],
      securityConfig: SecurityConfig
  ): Resource[F, HttpApi[F]] =
    Resource
      .eval(createAuthenticator[F](core.users, securityConfig))
      .map(authenticator => new HttpApi[F](core, authenticator))
}
