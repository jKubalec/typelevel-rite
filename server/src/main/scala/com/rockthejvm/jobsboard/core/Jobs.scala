package com.rockthejvm.jobsboard.core

import java.util.UUID
import cats.*
import cats.implicits.*
import cats.effect.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*
import org.typelevel.log4cats.Logger

import com.rockthejvm.jobsboard.logging.syntax.*
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.pagination.*

trait Jobs[F[_]] {

  //  "algebra"
  //  CRUD operations

  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all(): F[List[Job]] //  TODO: fix thoughts on the all() method
  def all(filter: JobFilter, pagination: Pagination): F[List[Job]]
  def find(id: UUID): F[Option[Job]]
  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]
  def delete(id: UUID): F[Int]
}

class LiveJobs[F[_]: MonadCancelThrow: Logger] private (xa: Transactor[F]) extends Jobs[F] {

  override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] =
    sql"""
      INSERT INTO jobs(
        date,
        ownerEmail,
        company,
        title,
        description,
        externalUrl,
        remoteFlag,
        location,
        salaryLo,
        salaryHi,
        currency,
        country,
        tags,
        image,
        seniority,
        other,
        active
      ) VALUES (
        ${System.currentTimeMillis()},
        ${ownerEmail},
        ${jobInfo.company},
        ${jobInfo.title},
        ${jobInfo.description},
        ${jobInfo.externalUrl},
        ${jobInfo.remoteFlag},
        ${jobInfo.location},
        ${jobInfo.salaryLo},
        ${jobInfo.salaryHi},
        ${jobInfo.currency},
        ${jobInfo.country},
        ${jobInfo.tags},
        ${jobInfo.image},
        ${jobInfo.seniority},
        ${jobInfo.other},
        false
      )
    """.update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(xa)

  override def all(): F[List[Job]] =
    sql"""
      SELECT 
        id,
        date,
        ownerEmail,
        company,
        title,
        description,
        externalUrl,
        remoteFlag,
        location,
        salaryLo,
        salaryHi,
        currency,
        country,
        tags,
        image,
        seniority,
        other,
        active
      FROM jobs  
    """
      .query[Job]
      .to[List]
      .transact(xa)

  override def all(filter: JobFilter, pagination: Pagination): F[List[Job]] = {
    val selectFragment: Fragment =
      fr"""
      SELECT 
        id,
        date,
        ownerEmail,
        company,
        title,
        description,
        externalUrl,
        remoteFlag,
        location,
        salaryLo,
        salaryHi,
        currency,
        country,
        tags,
        image,
        seniority,
        other,
        active
    """

    val fromFragment: Fragment =
      fr"FROM jobs"

    /*
      WHERE fragment:
      WHERE company in [filter.companies]
      AND location in [filter.locastions]
      ...
      AND (
        tag1=any(tags)
        OR tag2=any(tags)
        OR ...
      )
      ...
      AND remote = [filter.remote]
      AND salaryHi >= [filter.salary]
     */
    val whereFragment: Fragment = Fragments.whereAndOpt( //  Option[NonEmptyList] => Option[Fragment]
      filter.companies.toNel.map(companies =>
        Fragments.in(fr"company", companies)
      ), // Option["WHERE company in $companies"]
      filter.locations.toNel.map(locations => Fragments.in(fr"location", locations)),
      filter.countries.toNel.map(countries => Fragments.in(fr"country", countries)),
      filter.seniorities.toNel.map(seniorities => Fragments.in(fr"seniority", seniorities)),
      filter.tags.toNel.map(tags => //  intersection between filter.tags and row's tags
        Fragments.or(tags.toList.map(tag => fr"$tag=any(tags)"): _*)
      ),
      filter.maxSalary.map(salary => fr"salaryHi >= $salary"),
      filter.remote.some.map(remote => fr"remoteFlag = $remote")
    )

    val paginationFragment: Fragment =
      fr"ORDER BY id LIMIT ${pagination.limit} OFFSET ${pagination.offset}"

    val statement = selectFragment |+| fromFragment |+| whereFragment |+| paginationFragment

    Logger[F].info(statement.toString) *>
      statement
        .query[Job]
        .to[List]
        .transact(xa)
        .logError(e => "Failed euqery: ${e.getMessage}")
  }

  override def find(id: UUID): F[Option[Job]] =
    sql"""
    SELECT
      id, 
      date,
      ownerEmail,
      company,
      title,
      description,
      externalUrl,
      remoteFlag,
      location,
      salaryLo,
      salaryHi,
      currency,
      country,
      tags,
      image,
      seniority,
      other,
      active
    FROM jobs
    WHERE id = $id
    """
      .query[Job]
      .option
      .transact(xa)

  override def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
      UPDATE jobs
      SET
        company = ${jobInfo.company},
        title = ${jobInfo.title},
        description = ${jobInfo.description},
        externalUrl = ${jobInfo.externalUrl},
        remoteFlag = ${jobInfo.remoteFlag},
        location = ${jobInfo.location},
        salaryLo = ${jobInfo.salaryLo},
        salaryHi = ${jobInfo.salaryHi},
        currency = ${jobInfo.currency},
        country = ${jobInfo.country},
        tags = ${jobInfo.tags},
        image = ${jobInfo.image},
        seniority = ${jobInfo.seniority},
        other = ${jobInfo.other}
      WHERE id = $id  
    """.update.run
      .transact(xa)
      .flatMap(_ => find(id))

  override def delete(id: UUID): F[Int] =
    sql"""
      DELETE FROM jobs
      WHERE id = ${id}
    """.update.run
      .transact(xa)
}

object LiveJobs {
  //  needed reader for implicit conversion of tuple jobInfo into JobInfo
  given jobRead: Read[Job] = Read[
    (
        UUID,                 //  id
        Long,                 //  date
        String,               //  ownerEmail
        String,               //  company
        String,               //  title
        String,               //  description
        String,               //  externalUrl
        Boolean,              //  remoteFlag
        String,               //  location
        Option[Int],          //  salaryLo
        Option[Int],          //  salaryHi
        Option[String],       //  currency
        Option[String],       //  country
        Option[List[String]], //  tags
        Option[String],       //  image
        Option[String],       //  seniority
        Option[String],       //  other
        Boolean               //  active
    )
  ].map {
    case (
          id: UUID,
          date: Long,
          ownerEmail: String,
          company: String,
          title: String,
          description: String,
          externalUrl: String,
          remoteFlag: Boolean,
          location: String,
          salaryLo: Option[Int] @unchecked,
          salaryHi: Option[Int] @unchecked,
          currency: Option[String] @unchecked,
          country: Option[String] @unchecked,
          tags: Option[List[String]] @unchecked,
          image: Option[String] @unchecked,
          seniority: Option[String] @unchecked,
          other: Option[String] @unchecked,
          active: Boolean
        ) =>
      Job(
        id = id,
        date = date,
        ownerEmail = ownerEmail,
        JobInfo(
          company = company,
          title = title,
          description = description,
          externalUrl = externalUrl,
          remoteFlag = remoteFlag,
          location = location,
          salaryLo = salaryLo,
          salaryHi = salaryHi,
          currency = currency,
          country = country,
          tags = tags,
          image = image,
          seniority = seniority,
          other = other
        ),
        active = active
      )
  }

  def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveJobs[F]] =
    new LiveJobs[F](xa).pure[F] //  or return type could be Resource[F, LiveJobs[F]]
}
