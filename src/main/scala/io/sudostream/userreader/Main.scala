package io.sudostream.userreader

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.{Http, HttpConnectionContext}
import com.softwaremill.macwire.wire
import io.sudostream.userreader.api.http.HttpRoutes
import io.sudostream.userreader.api.kafka.StreamingComponents
import io.sudostream.userreader.config.{ActorSystemWrapper, ConfigHelper}
import io.sudostream.userreader.dao._

// running in IDE
// -Djavax.net.ssl.keyStore=/etc/ssl/cacerts
// -Djavax.net.ssl.trustStore=/etc/ssl/cacerts
// LOCAL_MONGO_DB=true
object Main extends App with MiniKubeHelper {

  lazy val configHelper: ConfigHelper = wire[ConfigHelper]
  lazy val streamingComponents = wire[StreamingComponents]

  lazy val httpRoutes: HttpRoutes = wire[HttpRoutes]
  lazy val mongoDbConnectionWrapper: MongoDbConnectionWrapper = wire[MongoDbConnectionWrapperImpl]
  lazy val usersDao: UserReaderDao = wire[MongoDbUserReaderDao]
  lazy val actorSystemWrapper: ActorSystemWrapper = wire[ActorSystemWrapper]
  lazy val mongoFindQueries: MongoFindQueriesProxy = wire[MongoFindQueriesImpl]

  implicit val theActorSystem: ActorSystem = actorSystemWrapper.system
  val logger = Logging(theActorSystem, getClass)
  implicit val executor = theActorSystem.dispatcher
  implicit val materializer = actorSystemWrapper.materializer

  setupHttp()

  private def setupHttp() {
    val httpInterface = configHelper.config.getString("http.interface")
    val httpPort = configHelper.config.getInt("http.port")

    val bindingFuture = Http().bindAndHandle(httpRoutes.routes, httpInterface, httpPort, HttpConnectionContext)
    logger.info(s"Listening on $httpInterface:$httpPort")
  }

}
