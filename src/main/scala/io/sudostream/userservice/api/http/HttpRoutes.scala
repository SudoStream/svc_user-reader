package io.sudostream.userservice.api.http

import java.time.Instant
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.kafka.scaladsl.Producer
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import io.sudostream.timetoteach.messages.events.SystemEvent
import io.sudostream.timetoteach.messages.systemwide.{SystemEventType, TimeToTeachApplication}
import io.sudostream.userservice.api.kafka.StreamingComponents
import io.sudostream.userservice.config.ActorSystemWrapper
import io.sudostream.userservice.dao.UserReaderDao
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class HttpRoutes(dao: UserReaderDao,
                 actorSystemWrapper: ActorSystemWrapper,
                 streamingComponents: StreamingComponents
                )
  extends Health {
  implicit val system: ActorSystem = actorSystemWrapper.system
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = actorSystemWrapper.materializer
  val log = system.log

  implicit val timeout = Timeout(30 seconds)

  val routes: Route = path("api" / "users") {
    get {
      val initialRequestReceived = Instant.now().toEpochMilli
      log.debug("Called 'api/users' and now getting All the E's and O's from the DAO")

      val usersFuture = dao.extractAllUsers

      Source.fromFuture(usersFuture)
        .map {
          elem =>
            log.info(s"Received all ${elem.size} users from the DAO")

            SystemEvent(
              eventType = SystemEventType.SCOTTISH_ES_AND_OS_REQUESTED_EVENT,
              requestFingerprint = UUID.randomUUID().toString,
              requestingSystem = TimeToTeachApplication.HTTP,
              requestingSystemExtraInfo = Option.empty,
              requestingUsername = Option.empty,
              originalUTCTimeOfRequest = initialRequestReceived,
              processedUTCTime = Instant.now().toEpochMilli,
              extraInfo = Option.empty
            )
        }
        .map {
          elem =>
            new ProducerRecord[Array[Byte], SystemEvent](streamingComponents.definedSystemEventsTopic, elem)
        }
        .runWith(Producer.plainSink(streamingComponents.producerSettings))

      onComplete(usersFuture) {
        case Success(users) =>
          complete(HttpEntity(ContentTypes.`application/json`, users.toString))
        case Failure(ex) => failWith(ex)
      }

    }
  } ~ health


}
