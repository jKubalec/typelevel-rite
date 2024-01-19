package com.rockthejvm.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import cats.effect.IO

final case class JobPage(id: String) extends Page {
  override def initCmd: Cmd[IO, Page.Msg] =
    Cmd.None //  TODO
  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) =
    (this, Cmd.None)
  override def view(): Html[Page.Msg] =
    div(s"Job detail page - id $id")
}
