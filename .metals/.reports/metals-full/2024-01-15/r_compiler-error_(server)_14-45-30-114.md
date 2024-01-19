file://<WORKSPACE>/src/main/scala/com/rockthejvm/jobsboard/core/Auth.scala
### dotty.tools.dotc.core.TypeError$$anon$1: bad parameter reference F at typer
the parameter is type F in class LiveAuth but the prefix <noprefix>
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
offset: 4360
uri: file://<WORKSPACE>/src/main/scala/com/rockthejvm/jobsboard/core/Auth.scala
text:
```scala
package com.rockthejvm.jobsboard.core

import cats.effect.*
import cats.implicits.*
import cats.data.OptionT
import concurrent.duration.DurationInt
import org.typelevel.log4cats.Logger
import tsec.authentication.JWTAuthenticator
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt
import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator
import tsec.common.SecureRandomId
import tsec.authentication.BackingStore

import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.auth.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.domain.user
import com.rockthejvm.jobsboard.config.*

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[User]]
  def signUp(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]]
  def delete(email: String): F[Boolean]

  //  allow password recovery
  def sendPasswordRecoveryToken(email: String): F[Unit]

  def recoverPasswordFromToken(email: String, token: String, newPassword: String): F[Boolean]
}

class LiveAuth[F[_]: Async: Logger] private (users: Users[F], tokens: Tokens[F], emails: Emails[F])
    extends Auth[F] {

  override def login(email: String, password: String): F[Option[User]] =
    for {
      //  find the user in the DB -> return None if no user
      maybeUser <- users.find(email)
      //  check password
      //  Option[User].filt4er(User => IO[Boolean]) => IO[Option[User]]
      maybeValidatedUser <- maybeUser.filterA(user =>
        BCrypt.checkpwBool[F](password, PasswordHash[BCrypt](user.hashedPassword))
      )
      //  return a new token if password matches
      // maybeJwtToken <- maybeValidatedUser.traverse(user => authenticator.create(user.email))
      //              Option[User].map(User => F[JWTToken]) => Option[F[JWTToken]]
    } yield maybeValidatedUser

  override def signUp(newUserInfo: NewUserInfo): F[Option[User]] = for {
    //  find the user in the DB, if we did => None
    maybeUser <- users.find(newUserInfo.email)
    result <- maybeUser match {
      case Some(_) => None.pure[F]
      case None =>
        for {
          //  hash the new password
          hashedPassword <- BCrypt.hashpw[F](newUserInfo.password)
          user <- User(
            newUserInfo.email,
            hashedPassword,
            newUserInfo.firstName,
            newUserInfo.lastName,
            newUserInfo.company,
            Role.RECRUITER
          ).pure[F]
          //  create a new user in the DB
          _ <- users.create(user)
        } yield Some(user)
    }
  } yield result

  override def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]] = {

    def checkAndUpdate(
        user: User,
        oldPassword: String,
        newPassword: String
    ): F[Either[String, Option[User]]] = {
      for {
        //  if user, check password
        passCheck <- BCrypt
          .checkpwBool[F](
            newPasswordInfo.oldPassword,
            PasswordHash[BCrypt](user.hashedPassword)
          )
        updateResult <-
          if (passCheck)
            updateUser(user, newPassword).map(Right(_))
          else Left("Invalid password").pure[F]
      } yield updateResult
    }

    users.find(email).flatMap {
      case None => Right(None).pure[F]
      case Some(user) =>
        val NewPasswordInfo(oldPassword, newPassword) = newPasswordInfo
        checkAndUpdate(user, oldPassword, newPassword)
    }
  }

  override def delete(email: String): F[Boolean] =
    users.delete(email)

  //  password recovery
  override def sendPasswordRecoveryToken(email: String): F[Unit] = {
    tokens.getToken(email).flatMap {
      case Some(token) => emails.sendPasswordRecoveryEmail(email, token)
      case None        => ().pure[F]
    }
  }
  override def recoverPasswordFromToken(
      email: String,
      token: String,
      newPassword: String
  ): F[Boolean] =
    for {
      maybeUser    <- users.find(email)
      tokenIsValid <- tokens.checkToken(email, token)
      result <- (maybeUser, tokenIsValid) match {
        case (Some(user), true) =>
          updateUser(user, newPassword).map(x => {
            @@println(s"recoverPasswordFromToken: $x"); x.nonEmpty
          })
        case _ => false.pure[F]
      }
    } yield result

    //  private

  def updateUser(user: User, newPassword: String): F[Option[User]] =
    for {
      //  if password ok, hash new password
      hashedPassword <- BCrypt.hashpw[F](newPassword)
      //  update
      updatedUser <- users.update(user.copy(hashedPassword = hashedPassword))
    } yield updatedUser
}

object LiveAuth {
  def apply[F[_]: Async: Logger](
      users: Users[F],
      tokens: Tokens[F],
      email: Emails[F]
  ): F[LiveAuth[F]] =
    new LiveAuth[F](users, tokens, email).pure[F]

}

```



#### Error stacktrace:

```

```
#### Short summary: 

dotty.tools.dotc.core.TypeError$$anon$1: bad parameter reference F at typer
the parameter is type F in class LiveAuth but the prefix <noprefix>
does not define any corresponding arguments.
idx = 0, args = ,
constraint =  uninstantiated variables:
 constrained types:
 bounds:
 ordering:
 co-deps:
 contra-deps:
