package com.rockthejvm.foundations

import cats.effect.{IO, IOApp, MonadCancelThrow}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor

object Doobie extends IOApp.Simple {

  import doobie.implicits._ //  for creating queries (sql"...")

  case class Student(id: Int, name: String)

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",                 //  JDBC connector
    "jdbc:postgresql://localhost:5432/demo", //  0r jdbc:postgresql:demo (only for localhost)
    "docker",                                //  username
    "docker"                                 //  password
  )

  //  read
  def findAllStudentNames: IO[List[String]] = {
    val query  = sql"select name from students".query[String]
    val action = query.to[List]
    action.transact(xa)
  }

  //  write
  def insertStudent(id: Int, name: String): IO[Int] = {
    val query  = sql"insert into students(id, name) values ($id, $name)"
    val action = query.update.run
    action.transact(xa)
  }

  //  read as case classes with fragments
  //  building SQL statements out of fragments
  def findStudentsByInitial(letter: String): IO[List[Student]] = {
    val selectPart = fr"select id, name"
    val fromPart   = fr"from students"
    val wherePart  = fr"where left(name, 1) = $letter"

    val statement = selectPart ++ fromPart ++ wherePart
    val action    = statement.query[Student].to[List]
    action.transact(xa)
  }

  //  organize code
  trait Students[F[_]] { //  "repository"
    def findById(id: Int): F[Option[Student]]
    def findAll: F[List[Student]]
    def create(name: String): F[Int]
  }

  object Students {
    def make[F[_]: MonadCancelThrow](xa: Transactor[F]): Students[F] = new Students[F] {
      def findById(id: Int): F[Option[Student]] =
        sql"select id, name from students where id=$id".query[Student].option.transact(xa)

      def findAll: F[List[Student]] =
        sql"select id, name from students".query[Student].to[List].transact(xa)

      def create(name: String): F[Int] =
        sql"insert into students(name) values ($name)".update
          .withUniqueGeneratedKeys[Int]("id")
          .transact(xa) //  automatic key generation (in Postgre)
    }
  }

  val postgresResource = for {
    ec <- ExecutionContexts.fixedThreadPool[IO](16)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",                 //  JDBC connector
      "jdbc:postgresql://localhost:5432/demo", //  0r jdbc:postgresql:demo (only for localhost)
      "docker",                                //  username
      "docker",                                //  password
      ec
    )
  } yield xa //  resource object

  val smallProgram = postgresResource.use { xa =>
    val studentsRepo = Students.make[IO](xa)
    for {
      id   <- studentsRepo.create("Petr")
      petr <- studentsRepo.findById(id)
      _    <- IO.println(s"First student is: $petr")
    } yield ()
  }

//  override def run: IO[Unit] = findAllStudentNames.map(println)
//  override def run: IO[Unit] = insertStudent(4, "yeeeet").map(println)
//  override def run: IO[Unit] = findStudentsByInitial("y").map(println)
  override def run: IO[Unit] = smallProgram
}
