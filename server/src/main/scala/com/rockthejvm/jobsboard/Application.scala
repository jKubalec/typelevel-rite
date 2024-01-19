package com.rockthejvm.jobsboard

import cats.effect.*
import com.rockthejvm.jobsboard.config.EmberConfig
import com.rockthejvm.jobsboard.http.routes.HealthRoutes
import org.http4s.dsl.*
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import com.rockthejvm.jobsboard.config.syntax.*
import com.rockthejvm.jobsboard.modules.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.rockthejvm.jobsboard.config.AppConfig

/*
  1 - add a plain health endpoint to our app
  2 - add minimal configuration
  3 - basic http server layout
 */

object Application extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] = ConfigSource.default.loadF[IO, AppConfig].flatMap {
    case AppConfig(postgresConfig, emberConfig, securityConfig, tokenConfig, emailserviceConfig) =>
      val appResource = for {
        xa      <- Database.makePostgresResource[IO](postgresConfig)
        core    <- Core[IO](xa, tokenConfig, emailserviceConfig)
        httpApi <- HttpApi[IO](core, securityConfig)
        server <- EmberServerBuilder
          .default[IO]
          .withHost(emberConfig.host)
          .withPort(emberConfig.port)
          .withHttpApp(httpApi.endpoints.orNotFound)
          .build
      } yield server

      appResource.use(_ => IO.println("Server ready!") *> IO.never)
  }
}
