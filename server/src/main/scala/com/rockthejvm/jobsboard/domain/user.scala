package com.rockthejvm.jobsboard.domain

import doobie.util.meta.Meta
import tsec.authorization.SimpleAuthEnum
import tsec.authorization.AuthGroup

import job.*

object user {
  final case class User(
      email: String,
      hashedPassword: String,
      firstName: Option[String],
      lastName: Option[String],
      company: Option[String],
      role: Role
  ) {
    def owns(job: Job): Boolean = email == job.ownerEmail
    def isAdmin: Boolean        = role == Role.ADMIN
    def isRecruiter: Boolean    = role == Role.RECRUITER
  }

  final case class NewUserInfo(
      email: String,
      password: String,
      firstName: Option[String],
      lastName: Option[String],
      company: Option[String]
  )

  enum Role {
    case ADMIN, RECRUITER
  }

  object Role {
    //  serves a purpose to store Role into DB as text
    given metaRole: Meta[Role] =
      Meta[String].timap[Role](Role.valueOf)(_.toString)
  }

  given roleAuthEnum: SimpleAuthEnum[Role, String] with {
    override val values: AuthGroup[Role]     = AuthGroup(Role.ADMIN, Role.RECRUITER)
    override def getRepr(role: Role): String = role.toString()
  }
}
