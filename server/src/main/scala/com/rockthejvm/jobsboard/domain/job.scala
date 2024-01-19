package com.rockthejvm.jobsboard.domain

import java.util.UUID

object job {

  //  IMPORTANT - define the order of fields IN THE BEGINNING! (once you have DB setup, that it's headache)

  case class Job(
      id: UUID,
      date: Long,
      ownerEmail: String,
      jobInfo: JobInfo,
      active: Boolean = false
  )

  case class JobInfo(
      company: String,
      title: String,
      description: String,
      externalUrl: String,
      remoteFlag: Boolean,
      location: String,
      salaryLo: Option[Int],
      salaryHi: Option[Int],
      currency: Option[String],
      country: Option[String],
      tags: Option[List[String]],
      image: Option[String],
      seniority: Option[String],
      other: Option[String]
  )

  object JobInfo {
    val empty: JobInfo =
      JobInfo("", "", "", "", false, "", None, None, None, None, None, None, None, None)

    def minimal(
        company: String,
        title: String,
        description: String,
        externalUrl: String,
        remoteFlag: Boolean,
        location: String
    ): JobInfo = JobInfo(
      company,
      title,
      description,
      externalUrl,
      remoteFlag,
      location,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None
    )
  }

  final case class JobFilter(
      companies: List[String] = List(),
      locations: List[String] = List(),
      countries: List[String] = List(),
      seniorities: List[String] = List(),
      tags: List[String] = List(),
      maxSalary: Option[Int] = None,
      remote: Boolean = false
  )
}
