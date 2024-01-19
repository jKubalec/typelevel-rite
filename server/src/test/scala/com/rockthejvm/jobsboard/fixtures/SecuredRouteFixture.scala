package com.rockthejvm.jobsboard.fixtures

import scala.concurrent.duration.DurationInt
import cats.effect.IO
import cats.data.OptionT
import org.http4s.Request
import org.http4s.Credentials
import org.http4s.AuthScheme
import org.http4s.headers.Authorization
import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator

import com.rockthejvm.jobsboard.domain.user.User
import com.rockthejvm.jobsboard.domain.security.*
import tsec.jws.mac.JWTMac
import tsec.authentication.SecuredRequestHandler

trait SecuredRouteFixture extends UsersFixture {
  val mockedAuthenticator: Authenticator[IO] = {
    //  key for hashing
    val key = HMACSHA256.unsafeGenerateKey //  implicits supplied by IO
    //  identity store to retrieve users
    //  basically map which returns user by their ID (String)
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == honzaEmail) OptionT.pure(Honza)
      else if (email == pavelEmail) OptionT.pure(Pavel)
      else OptionT.none[IO, User]
    // jwt authenticator
    JWTAuthenticator.unbacked.inBearerToken(
      1.day,   //  expiry of tokens,
      None,    //  max idle time
      idStore, //  identity store
      key      //  hash key
    )
  }

  extension (r: Request[IO]) {
    def withBearerToken(a: JwtToken): Request[IO] =
      r.putHeaders {
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](a.jwt)
        //  Authorization: Bearer {jwt}
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
      }
  }

  given securedHandler: SecuredHandler[IO] = SecuredRequestHandler(mockedAuthenticator)
}
