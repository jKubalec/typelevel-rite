package com.rockthejvm.jobsboard.domain

import cats.*
import cats.implicits.*
import tsec.authentication.AugmentedJWT
import tsec.mac.jca.HMACSHA256
import tsec.authentication.JWTAuthenticator
import tsec.authentication.SecuredRequest
import tsec.authorization.AuthorizationInfo
import tsec.authentication.TSecAuthService
import org.http4s.*
import org.http4s.headers.Authorization
import tsec.authorization.BasicRBAC
import tsec.authentication.SecuredRequestHandler

import com.rockthejvm.jobsboard.domain.user.*

object security {
  type Crypto   = HMACSHA256
  type JwtToken = AugmentedJWT[Crypto, String] //  Hash-based Message Authentication Code ...
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, Crypto]
  type AuthRBAC[F[_]]      = BasicRBAC[F, Role, User, JwtToken]
  //  type aliases for http routes
  type AuthRoute[F[_]]      = PartialFunction[SecuredRequest[F, User, JwtToken], F[Response[F]]]
  type SecuredHandler[F[_]] = SecuredRequestHandler[F, String, User, JwtToken]
  object SecuredHandler {
    def apply[F[_]](using handler: SecuredHandler[F]): SecuredHandler[F] = handler
  }

  //  types for RBAC  (role-base access control)
  //  BasicRBAC[F, Role, User, JwtToken]
  given authRole[F[_]: Applicative]: AuthorizationInfo[F, Role, User] with {
    override def fetchInfo(u: User): F[Role] = u.role.pure[F]
  }

  def allRoles[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC.all[F, Role, User, JwtToken]

  def recruiterOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.RECRUITER)

  def adminOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.ADMIN)

  //  authorization
  case class Authorizations[F[_]](rbacRoutes: Map[AuthRBAC[F], List[AuthRoute[F]]])
  object Authorizations {
    given combiner[F[_]]: Semigroup[Authorizations[F]] = Semigroup.instance { (authA, authB) =>
      Authorizations(authA.rbacRoutes |+| authB.rbacRoutes)
    }
  }

  //  AuthRoute -> Authorizations -> TSecAuthService -> HttpRoute
  // 1.  AuthRoute -> Authorizations = .restrictedTo extension method
  extension [F[_]](authRoute: AuthRoute[F]) {
    def restrictedTo(rbac: AuthRBAC[F]): Authorizations[F] =
      Authorizations(Map(rbac -> List(authRoute)))
  }

  //  2. Authorizations -> TSecAuthService = implicit conversion
  given auth2tsec[F[_]: Monad]
      : Conversion[Authorizations[F], TSecAuthService[User, JwtToken, F]] = { authz =>
    {
      //  this responds with 401 always
      val unauthorizedService: TSecAuthService[User, JwtToken, F] =
        TSecAuthService[User, JwtToken, F] { _ =>
          Response[F](Status.Unauthorized).pure[F]
        }

      authz.rbacRoutes //  Map[RBAC, List[AuthRoute[T]]]
        .toSeq
        .foldLeft(unauthorizedService) { case (acc, (rbac, routes)) =>
          //  merge routes into one
          val bigRoute = routes.reduce(_.orElse(_))
          //  build a new service, fall back to the acc if rbac/route fails
          TSecAuthService.withAuthorizationHandler(rbac)(bigRoute, acc.run)
        }
    }
  }

  //  3. semigroup for Authorization (in the companion object "Authorizations" ^^)
  //  to be able to combine multiple secured routes
}
