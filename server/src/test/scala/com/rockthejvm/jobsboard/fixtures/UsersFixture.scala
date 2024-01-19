package com.rockthejvm.jobsboard.fixtures

import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.core.Users
import cats.effect.IO

trait UsersFixture {

  val mockedUsers: Users[IO] = new Users[IO] {
    override def find(email: String): IO[Option[User]] =
      if (email == honzaEmail) IO.pure(Some(Honza))
      else IO.pure(None)
    override def create(user: User): IO[String]       = IO.pure(user.email)
    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
    override def delete(email: String): IO[Boolean]   = IO.pure(true)
  }

  /*
    Passwords:
      seznam => $2a$10$aZDWYgMMAR3wgI69j8jrquvyF553lxjSxydkApLs2N7oYy3ZH5ziG
      newpassword => $2a$10$zC64L.5HJKr8yZSzQKC7UuBu.KNKt7A42NXrNiabwdSM1xKetkf32
      pvaluvsvet => $2a$10$mbxwO/.Ojkml0S9pgDeNDui2On7bC9.GUHScjVQK3GOxIuIGNbf.e
      simplePass => $2a$10$DBJSl0b0HEwpCUpLxNc2xepFAw.V85F4plHKgZ8uEKI0FHXB.OvuK
   */

  val Honza = User(
    "mehere@email.cz",
    "$2a$10$aZDWYgMMAR3wgI69j8jrquvyF553lxjSxydkApLs2N7oYy3ZH5ziG",
    Some("Honza"),
    Some("Kub"),
    Some("Rock the JVM"),
    Role.ADMIN
  )

  val honzaEmail    = Honza.email
  val honzaPassword = "seznam"

  val UpdatedHonza = User(
    "mehere@email.cz",
    "$2a$10$zC64L.5HJKr8yZSzQKC7UuBu.KNKt7A42NXrNiabwdSM1xKetkf32",
    Some("HONZA"),
    Some("KUB"),
    Some("RTJVM"),
    Role.ADMIN
  )

  val NewUserHonza = NewUserInfo(
    honzaEmail,
    honzaPassword,
    Some("Honza"),
    Some("K"),
    Some("freelance")
  )

  val Pavel = User(
    "pavel@email.cz",
    "$2a$10$mbxwO/.Ojkml0S9pgDeNDui2On7bC9.GUHScjVQK3GOxIuIGNbf.e",
    Some("Pavel"),
    Some("Petr"),
    Some("Rock the JVM"),
    Role.RECRUITER
  )

  val pavelEmail    = Pavel.email
  val pavelPassword = "pvaluvsvet"

  val NewUserPavel = NewUserInfo(
    pavelEmail,
    pavelPassword,
    Some("Pavel"),
    Some("Petr"),
    Some("prezident")
  )
  val NewUser = User(
    "newuser@gmail.com",
    "$2a$10$DBJSl0b0HEwpCUpLxNc2xepFAw.V85F4plHKgZ8uEKI0FHXB.OvuK",
    Some("John"),
    Some("Doe"),
    Some("Some company"),
    Role.RECRUITER
  )
}
