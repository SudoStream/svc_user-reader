package io.sudostream.userreader.api.kafka

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.Materializer
import io.sudostream.timetoteach.kafka.serializing.SystemEventSerializer
import io.sudostream.userreader.config.{ActorSystemWrapper, ConfigHelper}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.ByteArraySerializer

import scala.concurrent.ExecutionContextExecutor

class StreamingComponents(configHelper: ConfigHelper, actorSystemWrapper: ActorSystemWrapper) {
  implicit val system: ActorSystem = actorSystemWrapper.system
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = actorSystemWrapper.materializer
  val log = system.log

  lazy val kafkaProducerBootServers = configHelper.config.getString("akka.kafka.producer.bootstrapservers")
  lazy val kafkaProducerSaslJaasUsername = configHelper.config.getString("akka.kafka.producer.saslJassUsername")
  lazy val kafkaProducerSaslJaasPassword = configHelper.config.getString("akka.kafka.producer.saslJassPassword")
  lazy val kafkaProducerSaslJaasConfig = s"org.apache.kafka.common.security.scram.ScramLoginModule required " +
    s"""username="$kafkaProducerSaslJaasUsername" password="$kafkaProducerSaslJaasPassword";"""

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new SystemEventSerializer)
    .withBootstrapServers(kafkaProducerBootServers)
    .withProperty(SaslConfigs.SASL_JAAS_CONFIG, kafkaProducerSaslJaasConfig)
    .withProperty(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-256")
    .withProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")

  definedSystemEventsTopic

  def definedSystemEventsTopic: String = {
    val sink_topic = configHelper.config.getString("user-reader.system_events_topic")
    log.info(s"Sink topic is '$sink_topic'")
    sink_topic
  }

}
