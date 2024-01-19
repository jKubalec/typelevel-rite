package com.rockthejvm.jobsboard

import tyrian.*
import tyrian.Html.*
import cats.effect.*
import scala.scalajs.js.annotation.*
import cats.effect.IO
import tyrian.cmds.Logger
import core.*
import org.scalajs.dom.window.*
import com.rockthejvm.jobsboard.components.*
import pages.*

object App {
  type Msg = Router.Msg | Page.Msg

  case class Model(router: Router, page: Page)
}

@JSExportTopLevel("RockTheJvmApp") // - this can be only ONCE per codebase (indicates the main app)
class App extends TyrianApp[App.Msg, App.Model] {
  import App.*
  /*
    We can send Messages by
    - trigger a command
    - create a subscription
    - listening for an event
   */

  // creates an initial Model - initial app state
  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    val location            = window.location.pathname
    val page                = Page.get(location)
    val pageCmd             = page.initCmd
    val (router, routerCmd) = Router.startAt(location)
    (Model(router, page), routerCmd |+| pageCmd)
  }

  //  potentially endless stream of messages (here of type Int)
  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.make( //  listener for browser history changes
      "urlChange",
      model.router.history.state.discrete
        .map(_.get)
        .map(newLocation => Router.ChangeLocation(newLocation, true))
    )

  //  model can change by receiving messages
  //  model => message => (new model, new command)
  //  update triggered whenever we get a new message
  //  usually handled with PF
  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case msg: Router.Msg =>
      val (newRouter, routerCmd) = model.router.update(msg)
      if (model.router == newRouter) //  no change is necessary
        (model, Cmd.None)
      else {
        //  location change, need to re-render the appropriate page
        val newPage    = Page.get(newRouter.location)
        val newPageCmd = newPage.initCmd
        (model.copy(router = newRouter, page = newPage), routerCmd |+| newPageCmd)
      }
    case msg: Page.Msg =>
      //  update the page
      val (newPage, cmd) = model.page.update(msg)
      (model.copy(page = newPage), cmd)
    case _ =>
      (model, Cmd.None) //  TODO to check external redirects as well
  }

  //  view triggered whenever model changes
  override def view(
      model: Model
  ): Html[Msg] = //  triggered every time model is changed. Model is changes via messages
    div(
      Header.view(),
      model.page.view()
    )
}
