package com.rockthejvm.jobsboard.core

import cats.effect.*
import cats.effect.implicits.*
import doobie.postgres.implicits.* //  sql"..." statement
import doobie.implicits.*

import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import com.rockthejvm.jobsboard.fixtures.JobFixture
import doobie.util.transactor
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.pagination.*

class JobsSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with DoobieSpec
    with JobFixture {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val initScript: String = "sql/jobs.sql"

  "Jobs 'algebra'" - {
    "should return no job if the given UUID does not exist" in {
      transactor.use { xa =>
        val program = for {
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.find(NotFoundJobUuid)
        } yield retrieved

        program.asserting(_ shouldBe None)
      }
    }

    "should retrieve a job by ID" in {
      transactor.use { xa =>
        val program = for {
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.find(AwesomeJobUuid)
        } yield retrieved

        program.asserting(_ shouldBe Some(AwesomeJob))
      }
    }

    "should retrieve all jobs" in {
      transactor.use { xa =>
        val program = for {
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.all()
        } yield retrieved

        program.asserting(_ shouldBe List(AwesomeJob))
      }
    }

    "should create a new job" in {
      transactor.use { xa =>
        val program = for {
          jobs     <- LiveJobs[IO](xa)
          jobId    <- jobs.create("me@email.com", RockTheJvmNewJob)
          maybeJob <- jobs.find(jobId)
        } yield maybeJob

        program.asserting(_.map(_.jobInfo) shouldBe Some(RockTheJvmNewJob))
      }
    }

    "should return an updated job if it exists" in {
      transactor.use { xa =>
        val program = for {
          jobs            <- LiveJobs[IO](xa)
          maybeUpdatedJob <- jobs.update(AwesomeJobUuid, UpdatedAwesomeJob.jobInfo)
        } yield maybeUpdatedJob

        program.asserting(_ shouldBe Some(UpdatedAwesomeJob))
      }
    }

    "should return None when trying to update a job that does not exist" in {
      transactor.use { xa =>
        val program = for {
          jobs            <- LiveJobs[IO](xa)
          maybeUpdatedJob <- jobs.update(NotFoundJobUuid, UpdatedAwesomeJob.jobInfo)
        } yield maybeUpdatedJob

        program.asserting(_ shouldBe None)
      }
    }

    "should delete an existing job" in {
      transactor.use { xa =>
        val program = for {
          jobs                <- LiveJobs[IO](xa)
          numberOfDeletedJobs <- jobs.delete(AwesomeJobUuid)
          countOfJobs <- sql"SELECT COUNT(*) FROM jobs WHERE id = $AwesomeJobUuid"
            .query[Int]
            .unique
            .transact(xa)
        } yield (numberOfDeletedJobs, countOfJobs)

        program.asserting { case (numberOfDeletedJobs, countOfJobs) =>
          numberOfDeletedJobs shouldBe 1
          countOfJobs shouldBe 0
        }
      }
    }

    "should return zero updated rows if deleting nonexisting job" in {
      transactor.use { xa =>
        val program = for {
          jobs                <- LiveJobs[IO](xa)
          numberOfDeletedJobs <- jobs.delete(NotFoundJobUuid)
        } yield numberOfDeletedJobs

        program.asserting(_ shouldBe 0)
      }
    }

    "should filter remote jobs" in {
      transactor.use { xa =>
        val program = for {
          jobs         <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(JobFilter(remote = true), Pagination.default)
        } yield filteredJobs

        program.asserting(_ shouldBe List())
      }
    }

    "should filter jobs by tags" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(
            JobFilter(tags = List("scala", "cats", "zio")),
            Pagination.default
          )
        } yield filteredJobs

        program.asserting(_ shouldBe List(AwesomeJob))
      }
    }
  }
}
