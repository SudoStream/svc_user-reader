package io.sudostream.userreader.api.kafka

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import io.sudostream.timetoteach.kafka.serializing.SystemEventDeserializer
import io.sudostream.userreader.config.{ActorSystemWrapper, ConfigHelper}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.scalatest.AsyncFlatSpec
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.ExecutionContextExecutor

class SystemEventsConsumerTest extends AsyncFlatSpec with MockitoSugar {
  //  Set KAFKA_SASL_JASS_USERNAME KAFKA_SASL_JASS_PASSWORD, KAFKA_BOOTSTRAP_SERVERS

//  val configHelper = new ConfigHelper
//  val actorSystemWrapper: ActorSystemWrapper = new ActorSystemWrapper(configHelper)
//  implicit val system: ActorSystem = actorSystemWrapper.system
//  implicit val executor: ExecutionContextExecutor = system.dispatcher
//  implicit val materializer: Materializer = actorSystemWrapper.materializer
//
//  lazy val kafkaProducerBootServers = configHelper.config.getString("akka.kafka.producer.bootstrapservers")
//  lazy val kafkaProducerSaslJaasUsername = configHelper.config.getString("akka.kafka.saslJassUsername")
//  lazy val kafkaProducerSaslJaasPassword = configHelper.config.getString("akka.kafka.saslJassPassword")
//  lazy val kafkaProducerSaslJaasConfig = s"org.apache.kafka.common.security.scram.ScramLoginModule required " +
//    s"""username="$kafkaProducerSaslJaasUsername" password="$kafkaProducerSaslJaasPassword";"""
//
//
//  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new SystemEventDeserializer)
//    .withBootstrapServers(kafkaProducerBootServers)
//    .withGroupId("SystemEventsConsumerTest")
//    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
//    .withProperty(SaslConfigs.SASL_JAAS_CONFIG, kafkaProducerSaslJaasConfig)
//    .withProperty(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-256")
//    .withProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
//
//  "Listening to system events topic" should "return list of size 2 users" in {
//    Consumer.committableSource(consumerSettings, Subscriptions.topics("h1t96dt8-SYSTEM_ALL_EVENTS_LOG"))
//      .map { msg =>
//        val theMsg = msg.record.value()
//        println(s"System Event : $theMsg")
//      }.runWith(Sink.ignore)
//
//    println("Sleeping ...")
//    Thread.sleep(30000L)
//    println("Done")
//
//    assert(1 === 1)
//  }

}
