package com.rockthejvm.jobsboard.core

import scala.concurrent.duration.DurationInt

import cats.effect.*
import cats.effect.implicits.*

import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import com.rockthejvm.jobsboard.fixtures.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.config.*

class TokensSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with DoobieSpec
    with Matchers
    with UsersFixture {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val initScript: String = "sql/recoverytokens.sql"

  "Tokens 'algebra'" - {
    "should not create a new token for a non-existing user" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(1000000L))
          token  <- tokens.getToken("somebody@someemail.com")
        } yield token

        program.asserting(_ shouldBe None)
      }
    }

    "should create a new token for an existing user" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(1000000L))
          token  <- tokens.getToken(honzaEmail)
        } yield token

        program.asserting(_ shouldBe defined)
      }
    }

    "should not validate expired tokens" in {
      transactor.use { xa =>
        val program = for {
          tokens     <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100L))
          maybeToken <- tokens.getToken(honzaEmail)
          _          <- IO.sleep(150.millis)
          isTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(honzaEmail, token)
            case None        => IO.pure(false)
          }
        } yield isTokenValid

        program.asserting(_ shouldBe false)
      }
    }

    "should validate not expired tokens" in {
      transactor.use { xa =>
        val program = for {
          tokens     <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100000L))
          maybeToken <- tokens.getToken(honzaEmail)
          _          <- IO.sleep(150.millis)
          isTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(honzaEmail, token)
            case None        => IO.pure(false)
          }
        } yield isTokenValid

        program.asserting(_ shouldBe true)
      }
    }

    "should only validate tokens for user who generated them" in {
      transactor.use { xa =>
        val program = for {
          tokens     <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100000L))
          maybeToken <- tokens.getToken(honzaEmail)
          isHonzaTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(honzaEmail, token)
            case None        => IO.pure(false)
          }
          isOtherTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken("someoneelse@gmail.com", token)
            case None        => IO.pure(false)
          }
        } yield (isHonzaTokenValid, isOtherTokenValid)

        program.asserting { case (isHonzaTokenValid, isOtherTokenValid) =>
          isHonzaTokenValid shouldBe true
          isOtherTokenValid shouldBe false
        }
      }
    }
  }
}
