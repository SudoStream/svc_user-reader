package io.sudostream.userservice

import com.softwaremill.macwire.wire
import io.sudostream.userservice.config.ConfigHelper

object Main extends App {
  lazy val configHelper: ConfigHelper = wire[ConfigHelper]
}
