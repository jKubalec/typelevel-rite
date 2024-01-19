file://<WORKSPACE>/src/test/scala/com/rockthejvm/jobsboard/http/routes/AuthRoutesSpec.scala
### java.lang.AssertionError: NoDenotation.owner

occurred in the presentation compiler.

action parameters:
offset: 9033
uri: file://<WORKSPACE>/src/test/scala/com/rockthejvm/jobsboard/http/routes/AuthRoutesSpec.scala
text:
```scala
package com.rockthejvm.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import cats.effect.*
import cats.implicits.*
import cats.data.OptionT
import scala.concurrent.duration.*

import java.util.UUID
import org.http4s.*
import org.http4s.implicits.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.http4s.Request
import org.typelevel.ci.CIStringSyntax

import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator
import tsec.jws.mac.JWTMac

import org.scalatest.freespec.AsyncFreeSpec

import com.rockthejvm.jobsboard.domain.user.User
import com.rockthejvm.jobsboard.fixtures.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.domain.auth.*
import com.rockthejvm.jobsboard.domain.security.*

class AuthRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with SecuredRouteFixture {

  /////////////////////////////////////////////////////////////////////
  // prep
  /////////////////////////////////////////////////////////////////////

  val mockedAuth: Auth[IO] = probedAuth(None)

  def probedAuth(userMap: Option[Ref[IO, Map[String, String]]]): Auth[IO] = new Auth[IO] {
    //  TODO: make sure ONLY Honza already exists
    override def login(email: String, password: String): IO[Option[User]] =
      if (email == honzaEmail && password == honzaPassword)
        IO(Some(Honza))
      else IO.pure(None)

    override def signUp(newUserInfo: NewUserInfo): IO[Option[User]] =
      if (newUserInfo.email == pavelEmail)
        IO.pure(Some(Pavel))
      else
        IO.pure(None)

    override def changePassword(
        email: String,
        newPasswordInfo: NewPasswordInfo
    ): IO[Either[String, Option[User]]] =
      if (email == honzaEmail)
        if (newPasswordInfo.oldPassword == honzaPassword)
          IO.pure(Right(Some(Honza)))
        else IO.pure(Left("Invalid password"))
      else IO.pure(Right(None))

    override def delete(email: String): IO[Boolean] = IO.pure(true)

    val authenticator: Authenticator[IO] = mockedAuthenticator

    override def sendPasswordRecoveryToken(email: String): IO[Unit] = {
      userMap
        .traverse { userMapRef =>
          userMapRef.modify { userMap =>
            (userMap + (email -> "abcd1234"), ())
          }
        } //  IO[Option[Unit]]
        .map(_ => ())
    }
    override def recoverPasswordFromToken(
        email: String,
        token: String,
        newPassword: String
    ): IO[Boolean] =
      userMap
        .traverse { userMapRef =>
          userMapRef.get
            .map { userMap =>
              userMap.get(email).filter(_ == token) //  Option[String]
            }                                       //  IO[Option[String]]
            .map(_.nonEmpty)                        //  IO[Boolean]
        }                                           //  IO[Option[Boolean]]
        .map(_.getOrElse(false))
  }

  given logger: Logger[IO]       = Slf4jLogger.getLogger[IO]
  val authRoutes: HttpRoutes[IO] = AuthRoutes[IO](mockedAuth, mockedAuthenticator).routes

/////////////////////////////////////////////////////////////////////
// tests
/////////////////////////////////////////////////////////////////////

  "AuthRoutes" - {
    "should returned a 401 - unauthorized if login fails" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(honzaEmail, "wrong-password"))
        )
      } yield {
        //  assertions here
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - OK + a JWT if login is successful" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(honzaEmail, honzaPassword))
        )
      } yield {
        //  assertions here
        response.status shouldBe Status.Ok
        response.headers.get(ci"Authorization") shouldBe defined
      }
    }

    "should return a 400 - Bad Request if the user already exists" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserHonza)
        )
      } yield {
        //  assertions here
        response.status shouldBe Status.BadRequest
      }
    }

    "should return a 201 - Created if the user creation succeeds" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserPavel)
        )
      } yield {
        //  assertions here
        response.status shouldBe Status.Created
      }
    }

    "should return a 200 - OK if logging out with a valid JWT token" in {
      for {
        jwtToken <- mockedAuthenticator.create(honzaEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
            .withBearerToken(jwtToken)
        )
      } yield {
        //  assertions here
        response.status shouldBe Status.Ok
      }
    }

    "should return a 401 - Unauthorized if logging out wihtout a JWT token" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
        )
      } yield {
        //  assertions here
        response.status shouldBe Status.Unauthorized
      }
    }

    //  change password - user doesn't exist => 404 Not Found
    "should return a 404 - Not Found if changing password for user that does not exist" in {
      for {
        jwtToken <- mockedAuthenticator.create(pavelEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(pavelPassword, "newpassword"))
        )
      } yield {
        //  assertions here
        response.status shouldBe Status.NotFound
      }
    }

    //  change password - invalid old password => 403 Forbidden
    "should return a 403 - Forbidden if old pasword is incorrect" in {
      for {
        jwtToken <- mockedAuthenticator.create(honzaEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo("wrong-password", "newpassword"))
        )
      } yield {
        //  assertions here
        response.status shouldBe Status.Forbidden
      }
    }

    //  change password - user JWT is invalid => 401 Unauthorized
    "should return a 401 - Unauthorized if changing password without a valid JWT" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordInfo(honzaPassword, "newpassword"))
        )
      } yield {
        //  assertions here
        response.status shouldBe Status.Unauthorized
      }
    }

    //  change password - happy path 200
    "should return a 200 - OK if changing password for a user with valid JWT and correct old password" in {
      for {
        jwtToken <- mockedAuthenticator.create(honzaEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(honzaPassword, "newpassword"))
        )
      } yield {
        //  assertions here
        response.status shouldBe Status.Ok
      }
    }

    "should return a 401 - Unauthorized if a non-admin user tries to delete auser" in {
      for {
        jwtToken <- mockedAuthenticator.create(pavelEmail) //  Pavel is not ADMIN
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/mehere@email.cz")
            .withBearerToken(jwtToken)
        )
      } yield {
        //  assertions here
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - Ok if an admin user tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(honzaEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/mehere@email.cz")
            .withBearerToken(jwtToken)
        )
      } yield {
        //  assertions here
        response.status shouldBe Status.Ok
      }
    }    
    
    "should return a 200 - Ok when resetting a password, and an email should be triggered" in {
      for {
        userMap <- Ref.of[IO, Matp[@@]]
        jwtToken <- mockedAuthenticator.create(honzaEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/mehere@email.cz")
            .withBearerToken(jwtToken)
        )
      } yield {
        //  assertions here
        response.status shouldBe Status.Ok
      }
    }
  }
}

```



#### Error stacktrace:

```
dotty.tools.dotc.core.SymDenotations$NoDenotation$.owner(SymDenotations.scala:2582)
	scala.meta.internal.pc.SignatureHelpProvider$.isValid(SignatureHelpProvider.scala:83)
	scala.meta.internal.pc.SignatureHelpProvider$.notCurrentApply(SignatureHelpProvider.scala:94)
	scala.meta.internal.pc.SignatureHelpProvider$.$anonfun$1(SignatureHelpProvider.scala:48)
	scala.collection.StrictOptimizedLinearSeqOps.loop$3(LinearSeq.scala:280)
	scala.collection.StrictOptimizedLinearSeqOps.dropWhile(LinearSeq.scala:282)
	scala.collection.StrictOptimizedLinearSeqOps.dropWhile$(LinearSeq.scala:278)
	scala.collection.immutable.List.dropWhile(List.scala:79)
	scala.meta.internal.pc.SignatureHelpProvider$.signatureHelp(SignatureHelpProvider.scala:48)
	scala.meta.internal.pc.ScalaPresentationCompiler.signatureHelp$$anonfun$1(ScalaPresentationCompiler.scala:388)
```
#### Short summary: 

java.lang.AssertionError: NoDenotation.owner