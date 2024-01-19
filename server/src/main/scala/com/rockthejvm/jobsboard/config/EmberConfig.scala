package com.rockthejvm.jobsboard.config

import com.comcast.ip4s.{Host, Port}
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import pureconfig.generic.derivation.default.*

//  generates given configReader: ConfigReader[EmberConfig]
final case class EmberConfig(host: Host, port: Port) derives ConfigReader

object EmberConfig {
  //  need given ConfigReader[Host] and given ConfigReader[Port]  =>  compiler generates ConfigReader[EmberConfig]
  given hostReader: ConfigReader[Host] = ConfigReader[String].emap { hostString =>
    Host
      .fromString(hostString)
      .toRight(
        CannotConvert(hostString, Host.getClass.toString, s"Invalid host string: $hostString")
      )
  //  ^^ replacement for ...
  /*
    match {
      case None => // error, return Left
        Left(CannotConvert(hostString, Host.getClass.toString, s"Invalid host string: $hostString"))
      case Some(host) => Right(host)
    }
   */
  }

  given portReader: ConfigReader[Port] = ConfigReader[Int].emap { portInt =>
    Port
      .fromInt(portInt)
      .toRight(
        CannotConvert(portInt.toString, Port.getClass.toString, s"Invalid port number: $portInt")
      )
  }
}
