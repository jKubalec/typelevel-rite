package com.rockthejvm.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import cats.effect.IO
import tyrian.cmds.Logger
import com.rockthejvm.jobsboard.common.Constants

//  form
/*
  NewUserInfo - input:
      - email
      - password
      - confirm password
      - first name
      - laste name
      - company
  button - "signup" trigger
 */
final case class SignupPage(
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    firstName: String = "",
    lastName: String = "",
    company: String = "",
    status: Option[Page.Status] = None
) extends Page {
  import SignupPage.*

  override def initCmd: Cmd[IO, Page.Msg] =
    Cmd.None
  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match {
    case UpdateEmail(email) =>
      Logger.consoleLog[IO](s"Update email $email!!!!")
      (this.copy(email = email), Cmd.None)
    case UpdatePassword(password) => (this.copy(password = password), Cmd.None)
    case ConfirmPassword(cp)      => (this.copy(confirmPassword = cp), Cmd.None)
    case UpdateFirstName(fn)      => (this.copy(firstName = fn), Cmd.None)
    case UpdateLastName(ln)       => (this.copy(lastName = ln), Cmd.None)
    case UpdateCompany(company)   => (this.copy(company = company), Cmd.None)
    case AttemptSignup =>
      Logger.consoleLog[IO]("AttemptSignup!!!!")
      if (!email.matches(Constants.emailRegex))
        (setErrorStatus("Email is invalid"), Cmd.None)
      else if (password.isEmpty())
        (setErrorStatus("Please enter a password"), Cmd.None)
      else if (password != confirmPassword)
        (setErrorStatus("Password fields do not match"), Cmd.None)
      else
        (this, Logger.consoleLog[IO]("SIGNING UP!", email, password, firstName, lastName, company))
    case _ => (this, Cmd.None)
  }

  override def view(): Html[Page.Msg] =
    div(`class` := "form-section")(
      //  title: Sign Up
      div(`class` := "top-section")(
        h1("Sign Up")
      ),
      form(
        name    := "signin",
        `class` := "form",
        onEvent(
          "submit",
          e => {
            e.preventDefault()
            NoOp
          }
        )
      )(
        //  6 inputs
        renderInput("Email", "email", "text", true, UpdateEmail(_)),
        renderInput("Password", "password", "password", true, UpdatePassword(_)),
        renderInput("Confirm Password", "confirmPassword", "password", true, ConfirmPassword(_)),
        renderInput("First Name", "firstName", "text", false, UpdateFirstName(_)),
        renderInput("Last Name", "lastName", "text", true, UpdateLastName(_)),
        renderInput("Company", "company", "text", true, UpdateCompany(_)),
        // button
        button(`type` := "button", onClick(UpdateEmail("aaa")))("Sign Up")
      )
    )

  /////////////////////////////////////////////////////////////////////////////////
  //  private
  /////////////////////////////////////////////////////////////////////////////////

  //  UI
  private def renderInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      onChange: String => Msg
  ) =
    div(`class` := "form-input")(
      label(`for` := name, `class` := "form-label")(
        if (isRequired) span("*") else span(),
        text(name)
      ),
      input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
    )

    //  util
  def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))
}

object SignupPage {
  trait Msg                                     extends Page.Msg
  case class UpdateEmail(email: String)         extends Msg
  case class UpdatePassword(password: String)   extends Msg
  case class ConfirmPassword(password: String)  extends Msg
  case class UpdateFirstName(firstName: String) extends Msg
  case class UpdateLastName(lastName: String)   extends Msg
  case class UpdateCompany(company: String)     extends Msg
  //  actions
  case object AttemptSignup extends Msg
  case object NoOp          extends Msg
}
