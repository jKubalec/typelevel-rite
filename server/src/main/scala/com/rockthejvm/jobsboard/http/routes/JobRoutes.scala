package com.rockthejvm.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import org.http4s.HttpRoutes
import org.http4s.dsl.*
import org.http4s.server.*
import tsec.authentication.asAuthed
import tsec.authentication.SecuredRequestHandler
import java.util.UUID
import scala.collection.mutable
import scala.language.implicitConversions

import com.rockthejvm.jobsboard.http.responses.FailureResponse
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.pagination.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.http.validation.syntax.*
import com.rockthejvm.jobsboard.domain.user.*

class JobRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (jobs: Jobs[F])
    extends HttpValidationDsl[F] { //  HttpValidationDsl is an add on to Http4Dsl[F] so that I have access to http4s implicits (like BadRequest)

  //  Debugging:
  import com.rockthejvm.jobsboard.logging.syntax.*

  //  "database"
  // private val database = mutable.Map[UUID, Job]()
  // that was used only for initial testing

  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object LimitQueryParam  extends OptionalQueryParamDecoderMatcher[Int]("limit")

  //  POST  /jobs?offset=x&limit=y { filters }  //  TODO: add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset) =>
      // Ok(database.values)
      for {
        filter   <- req.as[JobFilter]
        jobsList <- jobs.all(filter, Pagination(limit, offset))
        resp     <- Ok(jobsList)
      } yield resp
  }

  //  GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    // database.get(id) match {
    jobs.find(id).flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job $id not found."))
    }
  }

  //  HTTP payload validation
  //  possibility - use refined data types
  //  checked at compile time - increase compile time
  //  lowers DX "coerce"/*  */
  //  => we do not use it here

  //  other approach - implement own method "validateAs[...]"

  /*
  //  POST /jobs { jobInfo }
  //  historical function (kept here for study purps)
  private def createJob(jobInfo: JobInfo): F[Job] =
    Job(
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      ownerEmail = "TODO@rtjvm.com",
      jobInfo = jobInfo,
      active = true
    ).pure[F]

  //  http post /jobs/create
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        // _ <- Logger[F].info("Trying to add job")
        // _ <- Logger[F].info(s"Parsed job info: $jobInfo")
        // _ <- Logger[F].info(s"Created job: $job")
        // _ <- database.put(job.id, job).pure[F]
        // _ <- Logger[F].info(s"Job saved into DB: $job")
      } yield resp
  }
   */
  private val createJobRoute: AuthRoute[F] = { case req @ POST -> Root / "create" asAuthed _ =>
    req.request.validate[JobInfo] { jobInfo =>
      for {
        jobId <- jobs.create("TODO@rockthejvm.com", jobInfo)
        resp  <- Created(jobId)
      } yield resp
    }
  }

  //  PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: AuthRoute[F] = { case req @ PUT -> Root / UUIDVar(id) asAuthed user =>
    req.request.validate[JobInfo] { jobInfo =>
      jobs.find(id).flatMap {
        case None => NotFound(FailureResponse(s"Cannot update job $id: not found"))
        case Some(job) if user.owns(job) || user.isAdmin =>
          jobs.update(id, jobInfo) *> Ok()
        case _ => Forbidden(FailureResponse("You can only update your own jobs."))
      }
    }

  /*
    // historical testing version
      database.get(id) match {
        case Some(job) => for {
          jobInfo <- req.as[JobInfo]
          _ <- database.put(id, job.copy(jobInfo = jobInfo)).pure[F]
          resp <- Ok()
        } yield resp
        case None => NotFound(FailureResponse(s"Cannot update job $id: not found."))
      }
   */
  }

  //  DELETE /jobs/uuid
  private val deleteJobRoute: AuthRoute[F] = {
    case req @ DELETE -> Root / UUIDVar(id) asAuthed user =>
      jobs.find(id).flatMap {
        case None => NotFound(FailureResponse(s"Cannot delete job $id: not found"))
        case Some(job) if user.owns(job) || user.isAdmin =>
          for {
            _    <- jobs.delete(id)
            resp <- Ok()
          } yield resp
        case _ => Forbidden(FailureResponse("You can only delete your own jobs."))
      }
    /*
    // historical testing version
      database.get(id) match {
        case Some(job) => for {
          _ <- Logger[F].info(s"Found job to delete: ${job.id}")
          _ <- database.remove(id).pure[F]
          _ <- Logger[F].info(s"Job removed.")
          resp <- Ok()
        } yield resp
        case None => NotFound(FailureResponse(s"Cannot delete job $id: not found."))
      }
     */
  }

  val unauthedRoutes = (allJobsRoute <+> findJobRoute)
  val authedRoutes = SecuredHandler[F].liftService(
    createJobRoute.restrictedTo(allRoles) |+|
      updateJobRoute.restrictedTo(allRoles) |+|
      deleteJobRoute.restrictedTo(allRoles)
  )

  val routes = Router(
    "/jobs" -> (unauthedRoutes <+> authedRoutes)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler](jobs: Jobs[F]) =
    new JobRoutes[F](jobs)
}
