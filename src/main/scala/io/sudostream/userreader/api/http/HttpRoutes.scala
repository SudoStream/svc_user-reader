package io.sudostream.userreader.api.http

import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.kafka.scaladsl.Producer
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import io.sudostream.timetoteach.messages.events.SystemEvent
import io.sudostream.timetoteach.messages.systemwide.model.{SocialNetwork, User}
import io.sudostream.timetoteach.messages.systemwide.{SystemEventType, TimeToTeachApplication}
import io.sudostream.userreader.api.kafka.StreamingComponents
import io.sudostream.userreader.config.ActorSystemWrapper
import io.sudostream.userreader.dao.UserReaderDao
import org.apache.avro.io.{DatumWriter, EncoderFactory}
import org.apache.avro.specific.SpecificDatumWriter
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
  val log: LoggingAdapter = system.log

  implicit val timeout: Timeout = Timeout(30 seconds)

  private def convertToSocialNetworkName(socialNetworkString: String): SocialNetwork =
    socialNetworkString.toUpperCase match {

      case "FACEBOOK" => SocialNetwork.FACEBOOK
      case "GOOGLE" => SocialNetwork.GOOGLE
      case "TWITTER" => SocialNetwork.TWITTER
      case _ => SocialNetwork.OTHER
    }

  val routes: Route =
    path("api" / "user") {
      parameters('socialNetworkName, 'socialNetworkUserId) { (socialNetworkName, socialNetworkUserId) =>
        get {
          val initialRequestReceived = Instant.now().toEpochMilli
          log.debug(s"Looking for SocialNetwork ${socialNetworkName} & id ${socialNetworkUserId}")

          val userMaybe = dao.extractUserWithSocialIds(
            convertToSocialNetworkName(socialNetworkName),
            socialNetworkUserId)

          Source.fromFuture(userMaybe)
            .map {
              elem =>
                log.info(s"Received all ${elem.size} users from the DAO")

                SystemEvent(
                  eventType = SystemEventType.SPECIFIC_USER_REQUESTED,
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

          onComplete(userMaybe) {
            case Success(users) =>
              if (users.size == 1) {
                val user = users.head

                val writer: DatumWriter[User] = new SpecificDatumWriter[User](User.SCHEMA$)
                val out = new ByteArrayOutputStream()
                val encoder = new EncoderFactory().binaryEncoder(out, null)
                writer.write(user, encoder)
                encoder.flush()
                out.flush()
                out.close()
                val userBytes = out.toByteArray
                complete(HttpEntity(ContentTypes.`application/octet-stream`, userBytes))
              } else {
                reject
              }
            case Failure(ex) => failWith(ex)
          }
        }
      }
    } ~ path("api" / "users") {
      get {
        val initialRequestReceived = Instant.now().toEpochMilli
        log.debug("Called 'api/users' and now getting All the users from the DAO")

        val usersFuture = dao.extractAllUsers

        Source.fromFuture(usersFuture)
          .map {
            elem =>
              log.info(s"Received all ${elem.size} users from the DAO")

              SystemEvent(
                eventType = SystemEventType.ALL_USERS_REQUESTED,
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
