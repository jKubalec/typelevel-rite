package com.rockthejvm.jobsboard.core

import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.config.EmailServiceConfig
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.Message
import javax.mail.Transport

trait Emails[F[_]] {
  def sendEmail(to: String, subject: String, content: String): F[Unit]
  def sendPasswordRecoveryEmail(to: String, token: String): F[Unit]
}

class LiveEmails[F[_]: MonadCancelThrow] private (emailserviceConfig: EmailServiceConfig)
    extends Emails[F] {

  val host        = emailserviceConfig.host
  val port        = emailserviceConfig.port
  val user        = emailserviceConfig.user
  val password    = emailserviceConfig.password
  val frontendUrl = emailserviceConfig.frontendUrl

  //  API

  override def sendEmail(to: String, subject: String, content: String): F[Unit] = {
    val messageResource = for {
      prop    <- propsResource
      auth    <- authenticatorResource
      session <- createSession(prop, auth)
      message <- createMessage(session, "honza@email.cz", to, subject, content)
    } yield message

    messageResource.use(msg => Transport.send(msg).pure[F])
  }

  override def sendPasswordRecoveryEmail(to: String, token: String): F[Unit] = {
    val subject = "Jobs board password recovery"
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

    sendEmail(to, subject, content)
  }

  //  private

  val propsResource: Resource[F, Properties] = {
    val props = new Properties
    props.put("mail.smtp.auth", true)
    props.put("mail.smtp.starttls.enable", true)
    props.put("mail.smtp.host", host)
    props.put("mail.smtp.port", port)
    props.put("mail.smtp.ssl.trust", host)

    Resource.pure(props)
  }

  val authenticatorResource: Resource[F, Authenticator] = Resource.pure(new Authenticator {
    override protected def getPasswordAuthentication(): PasswordAuthentication =
      new PasswordAuthentication(user, password)
  })

  def createSession(props: Properties, auth: Authenticator): Resource[F, Session] =
    Resource.pure(Session.getInstance(props, auth))

  def createMessage(
      session: Session,
      from: String,
      to: String,
      subject: String,
      content: String
  ): Resource[F, MimeMessage] = {
    val message = new MimeMessage(session)
    message.setFrom(from)
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")
    Resource.pure(message)
  }
}

object LiveEmails {
  def apply[F[_]: MonadCancelThrow](emailserviceConfig: EmailServiceConfig): F[LiveEmails[F]] =
    new LiveEmails[F](emailserviceConfig).pure[F]
}
