package com.rockthejvm.jobsboard.modules

import cats.effect.*
import com.rockthejvm.jobsboard.config.*
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object Database {

  def makePostgresResource[F[_]: Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool(config.nThreads)
      xa <- HikariTransactor.newHikariTransactor[F](
        "org.postgresql.Driver",
        config.url,      // "jdbc:postgresql:board",
        config.user,     // "docker",
        config.password, // "docker",
        ec
      )
    } yield xa
}
