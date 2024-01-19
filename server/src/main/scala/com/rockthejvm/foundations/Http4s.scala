package com.rockthejvm.foundations

import cats.*
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.*
import cats.effect.{IO, IOApp}
import org.http4s.{Header, HttpRoutes}
import org.http4s.Headers._
import org.http4s.dsl._
import org.http4s.dsl.impl._
import org.http4s.server._
import org.http4s.dsl.io.{OptionalValidatingQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.ci.CIString

import java.util.UUID

object Http4s extends IOApp.Simple {

  //  simulate an HTTP server with "students" and "courses"

  type Student = String
  case class Instructor(firstName: String, lastName: String)
  case class Course(
      id: String,
      title: String,
      year: Int,
      students: List[Student],
      instructorName: String
  )

  object CourseRepository {
    //  a "database"

    val catsEffectCourse = Course(
      "35039ecc-d29b-4276-9b91-9a2953a0528e",
      "CE Scala course",
      2022,
      List("Honza", "Master Yoda"),
      "Daniel Cioc"
    )

    private val courses: Map[String, Course] = Map(catsEffectCourse.id -> catsEffectCourse)

    def findCoursesById(courseId: UUID): Option[Course] =
      courses.get(courseId.toString)

    def findCoursesByInstructor(name: String): List[Course] =
      courses.values.filter(_.instructorName == name).toList
  }

  //  essential REST endpoints
  //  GET localhost:8080/courses?instructor=Daniel%20Cioc&year=2022
  //  GET localhost:8080/courses/uuid=35039ecc-d29b-4276-9b91-9a2953a0528e/students

  object InstructorQueryParamMatcher extends QueryParamDecoderMatcher[String]("instructor")
  object YearQueryParamMatcher       extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  def courseRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(
            instructor
          ) +& YearQueryParamMatcher(maybeYear) =>
        val courses = CourseRepository.findCoursesByInstructor(instructor)
        maybeYear match {
          case Some(y) =>
            y.fold(
              _ => BadRequest(s"Parameter 'year' is invalid (provided value $maybeYear)"),
              year => Ok(courses.filter(_.year == year).asJson)
            )
          case None => Ok(courses.asJson)
        }
      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCoursesById(courseId).map(_.students) match {
          case Some(students) =>
            Ok(students.asJson, Header.Raw(CIString("My-custom-header"), "rockthejvm"))
          case None => NotFound(s"No course with courseId $courseId was found")
        }
    }
  }

  def healthEndpoint[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    HttpRoutes.of[F] { case GET -> Root / "health" =>
      Ok("All good here.")
    }
  }

  def allRoutes[F[_]: Monad]: HttpRoutes[F] = courseRoutes[F] <+> healthEndpoint[F]

  def routerWithPathPrefixes = Router(
    "/api"     -> courseRoutes[IO],
    "/private" -> healthEndpoint[IO]
  ).orNotFound

  override def run: IO[Unit] = EmberServerBuilder
    .default[IO]
//    .withHttpApp(courseRoutes[IO].orNotFound)
//    .withHttpApp(allRoutes[IO].orNotFound)
    .withHttpApp(routerWithPathPrefixes)
    .build
    .use(_ => IO.println("Server ready!") *> IO.never)
}
