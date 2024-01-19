package com.rockthejvm.jobsboard.playground

import tyrian.*
import tyrian.Html.*
import cats.effect.*
import scala.scalajs.js.annotation.*
import org.scalajs.dom.{document, console} //  brings JS console capabilities
import cats.effect.IO
import scala.concurrent.duration.*
import tyrian.cmds.Logger

object PlaygroundApp {
  sealed trait Msg
  case class Increment(amount: Int) extends Msg

  case class Model(count: Int)
}

// @JSExportTopLevel("RockTheJvmApp")   - this can be only ONCE per codebase (indicates the main app)
class PlaygroundApp extends TyrianApp[PlaygroundApp.Msg, PlaygroundApp.Model] {
  //                        ^^ Message  ^^ model="state"
  import PlaygroundApp.*
  /*
    We can send Messages by
    - trigger a command
    - create a subscription
    - listening for an event
   */

  // creates an initial Model - initial app state
  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    // ("", 1)
    (Model(0), Cmd.None)

  //  potentially endless stream of messages (here of type Int)
  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.every[IO](1.second).map(_ => Increment(1))
    // Sub.None

  //  model can change by receiving messages
  //  model => message => (new model, new command)
  //  update triggered whenever we get a new message
  //  usually handled with PF
  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = { case Increment(amount) =>
    // console.log(s"Changing count by $amount")    //  one way of logging
    // (model.copy(count = model.count + amount), Cmd.None)
    (
      model.copy(count = model.count + amount),
      Logger.consoleLog[IO]((s"Changing count by $amount"))
    ) //  another way of logging
  }
  // message => (model.copy(count = model.count + ), Cmd.None)

  //  view triggered whenever model changes
  override def view(
      model: Model
  ): Html[Msg] = //  triggered every time model is changed. Model is changes via messages
    div(
      button(onClick(Increment(1)))("Increase"),
      button(onClick(Increment(-1)))("Descrease"),
      div(s"Tyrian running: ${model.count}")
    )
}
