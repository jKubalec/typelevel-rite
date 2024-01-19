package com.rockthejvm.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.effect.*
import cats.implicits.*
import org.http4s.dsl.Http4sDsl

import org.http4s.*
import org.http4s.implicits.*
import org.http4s.Request
import java.util.UUID
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AsyncFreeSpec

import com.rockthejvm.jobsboard.fixtures.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.pagination.*

class JobRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture
    with SecuredRouteFixture {

  /////////////////////////////////////////////////////////////////////
  // prep
  /////////////////////////////////////////////////////////////////////

  val jobs: Jobs[IO] = new Jobs[IO] {

    override def all(): IO[List[Job]] = IO.pure(List(AwesomeJob))

    override def all(filter: JobFilter, pagination: Pagination): IO[List[Job]] =
      if (filter.remote) IO.pure(List())
      else IO.pure(List(AwesomeJob))

    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] = IO.pure(NewJobUuid)

    override def delete(id: UUID): IO[Int] =
      if (id == AwesomeJobUuid) IO.pure(1)
      else IO.pure(0)

    override def find(id: UUID): IO[Option[Job]] =
      if (id == AwesomeJobUuid)
        IO.pure(Some(AwesomeJob))
      else
        IO.pure(None)

    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] =
      if (id == AwesomeJobUuid)
        IO.pure(Some(UpdatedAwesomeJob))
      else
        IO.pure(None)
  }

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  //  this is what we actually test
  val jobRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs).routes

  /////////////////////////////////////////////////////////////////////
  // tests
  /////////////////////////////////////////////////////////////////////

  "JobRoutes" - {
    "should return a job with a given id" in {
      //  code under test
      for {
        //  simulate an HTTP request
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
        )
        //  get the HTTP response
        retrieved <- response.as[Job]
        //  make some assertions
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe AwesomeJob
      }
    }

    "should return all jobs" in {
      //  code under test
      for {
        //  simulate an HTTP request
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs")
            .withEntity(JobFilter()) //  empty filter - no filtering
        )
        //  get the HTTP response
        retrieved <- response.as[List[Job]]
        //  make some assertions
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List(AwesomeJob)
      }
    }

    "should return all jobs that satisfy a filter" in {
      //  code under test
      for {
        //  simulate an HTTP request
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs")
            .withEntity(JobFilter(remote = true))
        )
        //  get the HTTP response
        retrieved <- response.as[List[Job]]
        //  make some assertions
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List()
      }
    }

    "should create a new job" in {
      //  code under test
      for {
        //  authenticate
        jwtToken <- mockedAuthenticator.create(honzaEmail)
        //  simulate an HTTP request
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/create")
            .withEntity(AwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
        //  get the HTTP response
        retrieved <- response.as[UUID]
        //  make some assertions
      } yield {
        response.status shouldBe Status.Created
        retrieved shouldBe NewJobUuid
      }
    }

    "should only update a job that exists" in {
      //  code under test
      for {
        //  authenticate
        jwtToken <- mockedAuthenticator.create(honzaEmail)
        //  simulate an HTTP request
        responseOk <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
        //  get the HTTP response
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000")
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
        //  make some assertions
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }

    "should forbid the update a job that the JWT token does not 'own'" in {
      //  code under test
      for {
        //  authenticate
        jwtTokenUnauth <- mockedAuthenticator.create("someemail@post.com")
        //  simulate an HTTP request
        ResponseUnauth <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtTokenUnauth)
        )
        //  authenticate
        jwtTokenForbid <- mockedAuthenticator.create(pavelEmail)
        //  simulate an HTTP request
        ResponseForbid <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtTokenForbid)
        )
      } yield {
        //  make some assertions
        ResponseUnauth.status shouldBe Status.Unauthorized
        ResponseForbid.status shouldBe Status.Forbidden
      }
    }

    "should only delete a job that exists" in {
      //  code under test
      for {
        //  authenticate
        jwtToken <- mockedAuthenticator.create(honzaEmail)
        //  simulate an HTTP request
        responseOk <- jobRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
        //  get the HTTP response
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000")
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
        //  make some assertions
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }
  }

}
