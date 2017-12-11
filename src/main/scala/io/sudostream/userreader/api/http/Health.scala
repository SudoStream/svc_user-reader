package io.sudostream.userreader.api.http

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer

import scala.concurrent.ExecutionContextExecutor

trait Health {

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  val health: Route = path("health") {
    get {
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
        "<h1>Don't worry, she'll hold together... You hear me, baby? Hold together! 0.0.1-12</h1>\n"))
    }
  }

}
