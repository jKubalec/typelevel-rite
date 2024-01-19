package com.rockthejvm.jobsboard.playground

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.Message
import javax.mail.Transport
import cats.effect.*
import com.rockthejvm.jobsboard.core.LiveEmails
import com.rockthejvm.jobsboard.config.EmailServiceConfig

object EmailsPlayground {
  /*
    free "fake" email service - ethereal.email
   */
  def main(args: Array[String]): Unit = {
    //  configs

    //  generated (validity time limited)
    //  name = Peggie Hauck
    /*  Host 	smtp.ethereal.email
        Port 	587
        Security 	STARTTLS
        Username 	peggie.hauck83@ethereal.email
        Password 	6DtK22ubXCbWQdGJKG
     */
    val host        = "smtp.ethereal.email"
    val port        = 587
    val user        = "peggie.hauck83@ethereal.email"
    val password    = "6DtK22ubXCbWQdGJKG"
    val frontendUrl = "https://seznam.cz" //  final domain of our app

    val token = "ABCD1234"

    //  properties file
    /*
      mail.smtp.auth = true
      mail.smtp.starttls.enable = true
      mail.smtp.host = host
      mail.smtp.port = port
      mail.smtp.ssl.trust = host
     */
    val props = new Properties
    props.put("mail.smtp.auth", true)
    props.put("mail.smtp.starttls.enable", true)
    props.put("mail.smtp.host", host)
    props.put("mail.smtp.port", port)
    props.put("mail.smtp.ssl.trust", host)

    //  authentication
    val auth = new Authenticator {
      override protected def getPasswordAuthentication(): PasswordAuthentication =
        new PasswordAuthentication(user, password)
    }

    //  session
    val session = Session.getInstance(props, auth)

    //  email itself
    val subject = "Email from Jobs board"
    val content = s"""
    <div style="
      border: 1px solid black;
      padding: 20px;
      font-family: sans-serif;
      line-height: 2;
      font-size: 20px;
    ">
    <h1> Rock the JVM password recovery</h1>
    <p>Hello from Rock the JVM!</p>
    <p>your recovery token is: $token</p>
    <p>
      Click <a href="$frontendUrl/login"> link for password recovery </a>
    </p>
    <p>Hello from Rock the JVM!</p>
    </div>"""

    //  message = MIME message
    val message = new MimeMessage(session)
    message.setFrom("honza@seznam.cz")
    message.setRecipients(Message.RecipientType.TO, "some.user@gmail.com")
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")

    //  send
    Transport.send(message)
  }
}

object EmailsEffectPlayground extends IOApp.Simple {
  override def run: IO[Unit] = for {
    emails <- LiveEmails[IO](
      EmailServiceConfig(
        host = "smtp.ethereal.email",
        port = 587,
        user = "peggie.hauck83@ethereal.email",
        password = "6DtK22ubXCbWQdGJKG",
        frontendUrl = "https://seznam.cz"
      )
    )
    _ <- emails.sendPasswordRecoveryEmail("some@email.cz", "RTJVM12!")
  } yield ()
}
