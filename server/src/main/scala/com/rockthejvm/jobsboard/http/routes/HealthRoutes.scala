package com.rockthejvm.jobsboard.http.routes

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.server._
import org.http4s.dsl._

class HealthRoutes[F[_]: Monad] private extends Http4sDsl[F] {

  private val healthRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    Ok("All good here.")
  }

  val routes = Router(
    "/health" -> healthRoute
  )
}

object HealthRoutes {
  def apply[F[_]: Monad] = new HealthRoutes[F]
}
