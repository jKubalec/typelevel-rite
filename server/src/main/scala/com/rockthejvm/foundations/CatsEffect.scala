package com.rockthejvm.foundations

import cats.{Defer, MonadError}
import cats.effect.{
  Concurrent,
  Deferred,
  Fiber,
  GenSpawn,
  IO,
  IOApp,
  MonadCancel,
  Ref,
  Resource,
  Spawn,
  Sync,
  Temporal
}

import java.io.{File, FileWriter, PrintWriter}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*
import scala.io.{Source, StdIn}
import scala.util.Random
object CatsEffect extends IOApp.Simple {

  /*
    describing computations as values
   */

  //  IO = datastructure describing arbitrary computations (including side effects)
  val firstIO: IO[Int] = IO.pure(42)
  val delayedIO: IO[Int] = IO.apply {
    // complex code (effects)
    println("Crazy computation here...") //  is not printed unless evaluated
    42
  }

  def evaluateIO[A](io: IO[A]): Unit = {
    import cats.effect.unsafe.implicits.global //  "platform" for evaluating IOs (threadpool, executorcontext etc.)
    val meaningOfLife = io.unsafeRunSync()
    println(s"Result of the effect is: $meaningOfLife")
  }

  //  transformations
  //  map + flatMap
  val improverMeaningOfLie = firstIO.map(_ * 2)
  val printedMeaningOfLife = firstIO.flatMap(mol => IO(println(mol)))
  //  for-comprehensions
  def smallProgram(): IO[Unit] = for {
    line1 <- IO(StdIn.readLine())
    line2 <- IO(StdIn.readLine())
    _     <- IO(println(line1 + line2))
  } yield ()

  //  raise/"catch" errors
  val aFailure: IO[Int] = IO.raiseError(new RuntimeException("a proper failure"))
  val dealWithIt = aFailure.handleErrorWith { case _: RuntimeException =>
    IO(println("still here, no worriez..."))
  }

  //  fibers = "lightweight threads"
  val delayedPrint = IO.sleep(1.second) *> IO(println(Random.nextInt(100)))
  val manyPrints = for {
    fib1 <- delayedPrint.start
    fib2 <- delayedPrint.start
    _    <- fib1.join
    _    <- fib2.join
  } yield ()

  //  will not print (cancels before print)
  val cancelledFiber = for {
    fib <- delayedPrint.onCancel(IO(println("I'm cancelled"))).start
    _   <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
    _   <- fib.join
  } yield ()

  //  uncancellation
  //  will print - ignores the cancellation
  val ignoredCancellation = for {
    fib <- IO.uncancelable(_ => delayedPrint.onCancel(IO(println("I'm cancelled")))).start
    _   <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
    _   <- fib.join
  } yield ()

  //  resources
  val readingResource = Resource.make(
    IO(Source.fromFile("src/main/scala/com/rockthejvm/foundations/CatsEffect.scala"))
  )(source => IO(println("closing source")) *> IO(source.close()))
  val readingEffect = readingResource.use(source => IO(source.getLines().foreach(println)))

  //  compose resources
  val copiedFileResource = Resource.make(
    IO(new PrintWriter(new FileWriter(new File("src/main/resources/dupfile.scala"))))
  )(writer => IO(println("closing duplicated file")) *> IO(writer.close()))
  val compositeResource = for {
    source      <- readingResource
    destination <- copiedFileResource
  } yield (source, destination)

  val copyFileEffect = compositeResource.use { case (source, destination) =>
    IO(source.getLines().foreach(destination.println))
  }

  //  abstract kinds of computations

  //  MonacCancel = cancellable computations
  trait MyMonadCancel[F[_], E] extends MonadError[F, E] {
    trait CancellactionFlagResetter {
      def apply[A](fa: F[A]): F[A] //  with the cancellation flag reset
    }
    def canceled: F[Unit] //  cancelled computation
    def uncancellable[A](poll: CancellactionFlagResetter => F[A]): F[A]
  }

  //  monadCancel for IO
//  val monadCancelIO: MyMonadCancel[IO, Throwable] = MonadCancel[IO]
//  val uncancelableIO = monadCancelIO.uncancellable(_ => IO(42))   //  same as IO.uncancellable(...)

  //  Spawn = ability to create fibers
  trait MyGenSpawn[F[_], E] extends MonadCancel[F, E] {
    def start[A](fa: F[A]): F[Fiber[F, E, A]] //  creates a fiber
    //  never, cede, racePair...
  }

  trait MySpawn[F[_]] extends GenSpawn[F, Throwable]

  val spawnIO = Spawn[IO]
  val fiber   = spawnIO.start(delayedPrint) //  creates a fiber, same as delayedPrint.start

  //  Concurrent = concurrency primitives (atomic references + promises)
  trait MyConcurrenct[F[_]] extends Spawn[F] {
    def ref[A](a: A): F[Ref[F, A]]
    def deferred[A]: F[Deferred[F, A]] //  basically Promise
  }

  //  Temporal  = ability to suspend computations for a given time
  trait MyTemporal[F[_]] extends Concurrent[F] {
    def sleep(time: FiniteDuration): F[Unit]
  }

  //  Sync  = ability to suspend synchonous arbitrary expressions in a effect
  trait MySync[F[_]] extends MonadCancel[F, Throwable] with Defer[F] {
    def delay[A](expression: => A): F[A]
    def blocking[A](expression: => A): F[A] //  runs on a dedicated blocking thread pool
  }

  //  Async = ability to suspend asynchronous computations (i.e. on other thread pools) into an effect manager by CE
  trait MyAsync[F[_]] extends Sync[F] with Temporal[F] {
    def executionContext: F[ExecutionContext]
    def async[A](
        callback: (Either[Throwable, A] => Unit) => F[Option[F[Unit]]]
    ): F[A] //  most powerful CE API
  }

  /* Plain scala app style:
  def main(args: Array[String]): Unit = {
    println("starting")
//    evaluateIO(delayedIO)
    evaluateIO(smallProgram())
  }
   */

//  override def run: IO[Unit] = smallProgram()
//  override def run: IO[Unit] = manyPrints
//  override def run: IO[Unit] = cancelledFiber
//  override def run: IO[Unit] = ignoredCancellation
//  override def run: IO[Unit] = readingEffect
  override def run: IO[Unit] = copyFileEffect
}
