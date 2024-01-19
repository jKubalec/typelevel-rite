package com.rockthejvm.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import cats.effect.IO

final case class NotFoungPage() extends Page {
  override def initCmd: Cmd[IO, Page.Msg] =
    Cmd.None //  TODO
  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) =
    (this, Cmd.None)
  override def view(): Html[Page.Msg] =
    div("Ouch! page does not exist.")
}
