package com.rockthejvm.jobsboard.playground

import cats.effect.*
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash

object PasswordHashingPlayground extends IOApp.Simple {
  override def run: IO[Unit] =
    BCrypt.hashpw[IO]("scalarocks").flatMap(IO.println) *>
      BCrypt
        .checkpwBool[IO](
          "scalarocks",
          PasswordHash[BCrypt]("$2a$10$WUfQJIA.fOP.EzvA/wih9O6NSQoIxzPB6D5RIvSyxKcsgnG6WMuV.")
        )
        .flatMap(IO.println) *>
      IO.println("Seznam:") *>
      BCrypt.hashpw[IO]("seznam").flatMap(IO.println) *>
      BCrypt.hashpw[IO]("newpassword").flatMap(IO.println) *>
      BCrypt.hashpw[IO]("pvaluvsvet").flatMap(IO.println) *>
      BCrypt.hashpw[IO]("simplePass").flatMap(IO.println)

}
