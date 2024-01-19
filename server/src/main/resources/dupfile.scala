package com.rockthejvm.foundations

import cats.effect.{IO, IOApp, Resource}

import java.io.{File, FileWriter, PrintWriter}
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
    println("Crazy computation here...")  //  is not printed unless evaluated
    42
  }

  def evaluateIO[A](io: IO[A]): Unit = {
    import cats.effect.unsafe.implicits.global    //  "platform" for evaluating IOs (threadpool, executorcontext etc.)
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
    _ <- IO(println(line1 + line2))
  } yield ()

  //  raise/"catch" errors
  val aFailure: IO[Int] = IO.raiseError(new RuntimeException("a proper failure"))
  val dealWithIt = aFailure.handleErrorWith {
    case _: RuntimeException => IO(println("still here, no worriez..."))
  }

  //  fibers = "lightweight threads"
  val delayedPrint = IO.sleep(1.second) *>  IO(println(Random.nextInt(100)))
  val manyPrints = for {
    fib1 <- delayedPrint.start
    fib2 <- delayedPrint.start
    _ <- fib1.join
    _ <- fib2.join
  } yield ()

  //  will not print (cancels before print)
  val cancelledFiber = for {
    fib <- delayedPrint.onCancel(IO(println("I'm cancelled"))).start
    _ <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
    _ <- fib.join
  } yield ()

  //  uncancellation
  //  will print - ignores the cancellation
  val ignoredCancellation = for {
    fib <- IO.uncancelable(_ => delayedPrint.onCancel(IO(println("I'm cancelled")))).start
    _ <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
    _ <- fib.join
  } yield ()

  //  resources
  val readingResource = Resource.make(IO(Source.fromFile("src/main/scala/com/rockthejvm/foundations/CatsEffect.scala")))
                                  (source => IO(println("closing source")) *> IO(source.close()))
  val readingEffect = readingResource.use(source => IO(source.getLines().foreach(println)))

  //  compose resources
  val copiedFileResource = Resource.make(IO(new PrintWriter(new FileWriter(new File("src/main/resources/dupfile.scala"))))
  )(writer => IO(println("closing duplicated file")) *> IO(writer.close()))
  val compositeResource = for {
    source <- readingResource
    destination <- copiedFileResource
  } yield (source, destination)

  val copyFileEffect = compositeResource.use {
    case (source, destination) => IO(source.getLines().foreach(destination.println))
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
