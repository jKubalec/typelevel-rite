package com.rockthejvm.jobsboard.core

import cats.effect.*
import doobie.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.rockthejvm.jobsboard.fixtures.*
import com.rockthejvm.jobsboard.domain.user.*
import org.postgresql.util.PSQLException
import org.scalatest.Inside

class UsersSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Inside
    with DoobieSpec
    with UsersFixture {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override val initScript: String = "sql/users.sql"

  "Users 'algebra'" - {
    "should retrieve a user by email" in {
      transactor.use {
        xa => //  careful - every time transactor.use is called it creates a new transactor (i.e. calling transactor.use more times in one test leads potentially to loss of test data)
          val program = for {
            users     <- LiveUsers[IO](xa)
            retrieved <- users.find("pavel@email.cz")
          } yield retrieved

          program.asserting(_ shouldBe Some(Pavel))
      }
    }

    "should return None for nonexistent email" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          retrieved <- users.find("does_not_exist@email.cz")
        } yield retrieved

        program.asserting(_ shouldBe None)
      }
    }

    "should create a new user" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          userId <- users.create(NewUser)
          maybeUser <- sql"SELECT * FROM users WHERE email = ${NewUser.email}"
            .query[User]
            .option
            .transact(xa)
        } yield (userId, maybeUser)

        program.asserting { case (userId, maybeUser) =>
          userId shouldBe NewUser.email
          maybeUser shouldBe Some(NewUser)
        }
      }
    }

    "should fail creating a new user if the email already exists" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          userId <- users.create(Honza).attempt //  IO[Either[Throwable, String]]
        } yield userId

        program.asserting { outcome =>
          inside(outcome) {
            case Left(e) => e shouldBe a[PSQLException]
            case _       => fail()
          }
        }
      }
    }

    "should return None when updating a user that does not exist" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          maybeUser <- users.update(NewUser)
        } yield maybeUser

        program.asserting(_ shouldBe None)
      }
    }

    "should update an existing user" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          maybeUser <- users.update(UpdatedHonza)
        } yield maybeUser

        program.asserting(_ shouldBe Some(UpdatedHonza))
      }
    }

    "should delete an existing user" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          result <- users.delete(Pavel.email)
          maybeUser <- sql"SELECT * FROM users WHERE email = ${Pavel.email}"
            .query[User]
            .option
            .transact(xa)
        } yield (result, maybeUser)

        program.asserting { case (result, maybeUser) =>
          result shouldBe true
          maybeUser shouldBe None
        }
      }
    }

    "should not delete a nonexistent user" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          result <- users.delete("nobody@gmail.com")
        } yield result

        program.asserting(_ shouldBe false)

      }
    }
  }
}
