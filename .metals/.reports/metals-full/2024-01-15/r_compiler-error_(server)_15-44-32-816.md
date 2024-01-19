file://<WORKSPACE>/src/main/scala/com/rockthejvm/jobsboard/http/routes/AuthRoutes.scala
### dotty.tools.dotc.core.TypeError$$anon$1: bad parameter reference F at typer
the parameter is type F in class AuthRoutes but the prefix <noprefix>
does not define any corresponding arguments.
idx = 0, args = ,
constraint =  uninstantiated variables:
 constrained types:
 bounds:
 ordering:
 co-deps:
 contra-deps:


occurred in the presentation compiler.

action parameters:
offset: 3898
uri: file://<WORKSPACE>/src/main/scala/com/rockthejvm/jobsboard/http/routes/AuthRoutes.scala
text:
```scala
package com.rockthejvm.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import org.http4s.server.Router
import org.http4s.HttpRoutes
import org.http4s.Response
import org.http4s.Status
import tsec.authentication.asAuthed
import tsec.authentication.SecuredRequestHandler
import tsec.authentication.TSecAuthService

import com.rockthejvm.jobsboard.http.validation.syntax.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.auth.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.http.responses.*
import org.http4s.dsl.impl.Responses.ForbiddenOps

class AuthRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (
    auth: Auth[F],
    authenticator: Authenticator[F]
) extends HttpValidationDsl[F] {

  //  POST  /auth/login { LoginInfo } => OK with Authorization: Bearer {jwt}
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    req.validate[LoginInfo] { loginInfo =>
      val maybeJwtToken = for {
        maybeUser <- auth.login(loginInfo.email, loginInfo.password)
        _         <- Logger[F].info(s"User logging in: ${loginInfo.email}")

        //  TODO: create a new token
        maybeToken <- maybeUser.traverse(user => authenticator.create(user.email))
        //  Option[User].map(User => F[JWTToken]) => Option[F[JWTToken]]

      } yield maybeToken

      maybeJwtToken.map {
        case Some(token) =>
          authenticator.embed(Response(Status.Ok), token) //  Authorization: Bearer ...
        case None =>
          Response(Status.Unauthorized)
      }
    }
  }

  //  create a new user
  //  POST  /auth/users { NewUserInfo } => 201 Created or BadRequest
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "users" =>
      req.validate[NewUserInfo] { newUserInfo =>
        for {
          maybeNewUser <- auth.signUp(newUserInfo)
          resp <- maybeNewUser match {
            case Some(user) => Created(user.email)
            case None       => BadRequest(s"User with email ${newUserInfo.email} already exists")
          }
        } yield resp
      }
  }

  //  change a password
  //  PUT   /auth/users/password { NewPasswordInfo } { Authorization: Bearer {jwt} } => 200 OK
  private val changePasswordRoute: AuthRoute[F] = {
    case req @ PUT -> Root / "users" / "password" asAuthed user =>
      req.request.validate[NewPasswordInfo] { newPasswordInfo =>
        for {
          maybeUserOrError <- auth.changePassword(user.email, newPasswordInfo)
          resp <- maybeUserOrError match {
            case Right(Some(_)) => Ok()
            case Right(None) =>
              NotFound(FailureResponse(s"User ${user.email} not found")) //  theoretically impossible
            case Left(_) => Forbidden()
          }
        } yield resp
      }
  }

  //  POST  /auth/logout { Authorization: Bearer {jwt} } => 200 OK
  private val logoutRoute: AuthRoute[F] = { case req @ POST -> Root / "logout" asAuthed _ =>
    val token = req.authenticator
    for {
      _    <- authenticator.discard(token)
      resp <- Ok()
    } yield resp
  }

  //  DELETE  /auth/users/honza@email.cz  (should be possible only for admin)
  private val deleteUserRoute: AuthRoute[F] = {
    case req @ DELETE -> Root / "users" / email asAuthed user =>
      //  auth - delete user
      auth.delete(email).flatMap {
        case true  => Ok()
        case false => NotFound()
      }
  }

  //  POST /auth/reset { ForgotPasswordInfo } ~ email
  private val forgotPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "reset" =>
      for {
        fpInfo <- req.as[ForgotPasswordInfo]
        _      <- auth.sendPasswor@@dRecoveryToken(fpInfo.email)
        resp   <- Ok()
      } yield resp
  }

  //  POST /auth/recover { RecoverPasswordInfo }
  private val recoverPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "recover" =>
      for {
        rpInfo <- req.as[RecoverPasswordInfo]
        recoverySuccessful <- auth.recoverPasswordFromToken(
          rpInfo.email,
          rpInfo.token,
          rpInfo.newPassword
        )
        resp <-
          if (recoverySuccessful) Ok()
          else Forbidden(FailureResponse("Email/token combination is incorrect."))
      } yield resp
  }

  val unauthedRoutes =
    loginRoute <+> createUserRoute <+> forgotPasswordRoute <+> recoverPasswordRoute
  val authedRoutes =
    SecuredHandler[F].liftService( //  this heavily uses implicits from security.scala
      changePasswordRoute.restrictedTo(allRoles) |+|
        logoutRoute.restrictedTo(allRoles) |+|
        deleteUserRoute.restrictedTo(adminOnly)
    )

  val routes = Router(
    "/auth" -> (unauthedRoutes <+> authedRoutes)
  )
}

object AuthRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler]
  /* (SecuredHandler) - tagless final approach - we utilize the capabilities, not necessary use the results of the ops*/
  /* instantiate ONCE in the entire app */
  (
      auth: Auth[F],
      authenticator: Authenticator[F]
  ) = new AuthRoutes[F](auth, authenticator)
}

```



#### Error stacktrace:

```

```
#### Short summary: 

dotty.tools.dotc.core.TypeError$$anon$1: bad parameter reference F at typer
the parameter is type F in class AuthRoutes but the prefix <noprefix>
does not define any corresponding arguments.
idx = 0, args = ,
constraint =  uninstantiated variables:
 constrained types:
 bounds:
 ordering:
 co-deps:
 contra-deps:
