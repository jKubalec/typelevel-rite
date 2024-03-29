package com.rockthejvm.jobsboard.core

import tyrian.*
import cats.effect.IO
import fs2.dom.History //  fs2 wrappe over JS native History object

//  jobs.rockthejvm.com/somepath/12  (location = somepath/12)
case class Router private (location: String, history: History[IO, String]) {
  import Router.*

  def update(msg: Msg): (Router, Cmd[IO, Msg]) = msg match {
    case ChangeLocation(newLocation, browserTriggered) =>
      if (location == newLocation) (this, Cmd.None)
      else {
        val historyCmd =
          if (browserTriggered)
            Cmd.None             //  browser action (back), no need to push location on istory
          else goto(newLocation) //  manual action, need to push location
        (this.copy(location = newLocation), historyCmd)
      }
    case _: Msg => (this, Cmd.None)
  }

  def goto[M](location: String): Cmd[IO, M] =
    Cmd.SideEffect[IO] {
      history.pushState(location, location)
    }
}

object Router {
  trait Msg
  case class ChangeLocation(newLocation: String, browserTriggered: Boolean = false) extends Msg
  case class ExternalRedirect(location: String)                                     extends Msg

  def startAt[M](initialLocation: String): (Router, Cmd[IO, M]) = {
    val router = Router(initialLocation, History[IO, String])
    (router, router.goto(initialLocation))
  }
}
