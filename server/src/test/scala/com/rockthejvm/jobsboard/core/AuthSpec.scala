package com.rockthejvm.jobsboard.core

import concurrent.duration.DurationInt
import cats.effect.*
import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import com.rockthejvm.jobsboard.fixtures.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.domain.security.*

import com.rockthejvm.jobsboard.domain.auth.*
import com.rockthejvm.jobsboard.fixtures.*
import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import cats.data.OptionT
import tsec.authentication.JWTAuthenticator
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import com.rockthejvm.jobsboard.config.SecurityConfig

class AuthSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UsersFixture {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val mockedConfig: SecurityConfig = SecurityConfig("secret", 1.day)

  val mockedTokens: Tokens[IO] = new Tokens[IO] {
    override def getToken(email: String): IO[Option[String]] =
      if (email == honzaEmail) IO.pure(Some("abc1234"))
      else IO.pure(None)

    override def checkToken(email: String, token: String): IO[Boolean] =
      IO.pure(token == "abc1234")
  }

  val mockedEmails: Emails[IO] = new Emails[IO] {
    override def sendEmail(to: String, subject: String, content: String): IO[Unit] = IO.unit
    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit]    = IO.unit
  }

  def probedEmails(users: Ref[IO, Set[String]]): Emails[IO] = new Emails[IO] {
    override def sendEmail(to: String, subject: String, content: String): IO[Unit] =
      users.modify(set => (set + to, ()))

    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit] =
      sendEmail(to, "your token", "token")

    override def toString(): String = {
      val usersIO = for {
        usrs <- users.get
      } yield usrs.mkString(", ")
      usersIO.unsafeRunSync()
    }
  }
  /*
  val mockedAuthenticator: Authenticator[IO] = {
    //  key for hashing
    val key = HMACSHA256.unsafeGenerateKey //  implicits supplied by IO
    //  identity store to retrieve users
    //  basically map which returns user by their ID (String)
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == honzaEmail) OptionT.pure(Honza)
      else if (email == pavelEmail) OptionT.pure(Pavel)
      else OptionT.none[IO, User]
    // jwt authenticator
    JWTAuthenticator.unbacked.inBearerToken(
      1.day,   //  expiry of tokens,
      None,    //  max idle time
      idStore, //  identity store
      key      //  hash key
    )
  }
   */

  "Auth 'algebra'" - {
    "login should return None if the user does not exist" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login("user@rtjvm.com", "password")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return None if the user exists but the password is wrong" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login(honzaEmail, "wrongpassword")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return token if the user exists and password is correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login(
          honzaEmail,
          "seznam"
        )
      } yield maybeToken

      program.asserting(_ shouldBe defined)
    }

    "signing up should not create a user with an existing email" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeUser <- auth.signUp(
          NewUserInfo(
            honzaEmail,
            "somepassword",
            Some("Honza"),
            Some("lastname"),
            Some("company")
          )
        )
      } yield maybeUser

      program.asserting(_ shouldBe None)
    }

    "signing up should create a completely ne user" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeUser <- auth.signUp(
          NewUserInfo(
            "bob@rtjvm.com",
            "somepassword",
            Some("Bob"),
            Some("Jones"),
            Some("Some Company")
          )
        )
      } yield maybeUser

      program.asserting {
        case Some(user) =>
          user.email shouldBe "bob@rtjvm.com"
          user.firstName shouldBe Some("Bob")
          user.lastName shouldBe Some("Jones")
          user.company shouldBe Some("Some Company")
          user.role shouldBe Role.RECRUITER
        case _ =>
          fail()
      }
    }

    "change password should return Right(None) if the user doesn't exist" in {
      val program = for {
        auth   <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword("alice@rtjvm.com", NewPasswordInfo("oldpwd", "newpwd"))
      } yield result

      program.asserting(_ shouldBe Right(None))
    }

    "change password should correctly change password if all the details are correct" in {
      val program = for {
        auth   <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword(honzaEmail, NewPasswordInfo("seznam", "google"))
        isNicePassword <- result match {
          case Right(Some(user)) =>
            BCrypt
              .checkpwBool[IO](
                "google",
                PasswordHash[BCrypt](user.hashedPassword)
              )
          case _ =>
            IO.pure(false)
        }
      } yield isNicePassword

      program.asserting(_ shouldBe true)
    }

    "recover password should fail for a user that does not exist even if the token is correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result1 <- auth.recoverPasswordFromToken(
          "someone@rtjvm.com",
          "abc1234",
          "new-password-text"
        )
        result2 <- auth.recoverPasswordFromToken(
          "someone@rtjvm.com",
          "wrongToken",
          "new-password-text"
        )
      } yield (result1, result2)

      program.asserting(_ shouldBe (false, false))
    }

    "recover password should fail for a user that do exist but the token is wrong" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPasswordFromToken(
          honzaEmail,
          "wrong-token",
          "new-password-text"
        )
      } yield result

      program.asserting(_ shouldBe false)
    }

    "recover password should succeed for a correct combination of user/token" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPasswordFromToken(
          honzaEmail,
          "abc1234",
          "new-password-text"
        )
      } yield result

      program.asserting(_ shouldBe true)
    }

    "sending recovery passwords should fail for a user that does not exist" in {
      val program = for {
        set                  <- Ref.of[IO, Set[String]](Set())
        emails               <- IO(probedEmails(set))
        auth                 <- LiveAuth[IO](mockedUsers, mockedTokens, emails)
        _                    <- auth.sendPasswordRecoveryToken("anyone@whatever.com")
        usersBeingSentEmails <- set.get
      } yield usersBeingSentEmails

      program.asserting(_ shouldBe empty)
    }

    "sending recovery passwords should succeed for a user that exists" in {
      val program = for {
        set                  <- Ref.of[IO, Set[String]](Set())
        emails               <- IO(probedEmails(set))
        auth                 <- LiveAuth[IO](mockedUsers, mockedTokens, emails)
        result               <- auth.sendPasswordRecoveryToken(honzaEmail)
        usersBeingSentEmails <- set.get
      } yield usersBeingSentEmails

      program.asserting(_ should contain(honzaEmail))
    }
  }
}
