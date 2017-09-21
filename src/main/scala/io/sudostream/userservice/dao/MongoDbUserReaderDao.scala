package io.sudostream.userservice.dao

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.Materializer
import io.sudostream.timetoteach.messages.systemwide.model.{User, UserRole}
import io.sudostream.userservice.config.ActorSystemWrapper
import org.mongodb.scala.Document

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try

class MongoDbUserReaderDao(mongoFindQueriesProxy: MongoFindQueriesProxy,
                           actorSystemWrapper: ActorSystemWrapper) extends UserReaderDao {

  implicit val system: ActorSystem = actorSystemWrapper.system
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = actorSystemWrapper.materializer
  val log: LoggingAdapter = system.log

  def createUserFromMongoDocument(singleUserDoc: Document): Try[User] = {
    Try {

      val theTimeToTeachId: String = singleUserDoc.getString("_id")
      val theFullName: String = singleUserDoc.getString("fullName")
      val theGivenName: Option[String] = singleUserDoc.get[String]("givenName")
      val theFamilyName: Option[String] = singleUserDoc.get[String]("familyName")
      val theImageUrl: Option[String] = singleUserDoc.get[String]("imageUrl")
      // val theEmails = TODO
      val theUserRole: UserRole = singleUserDoc.getString("userRole") match {
        case "TEACHER" => UserRole.TEACHER
        case "ADMIN" => UserRole.ADMIN
        case _ => UserRole.OTHER
      }
      // val theSchools = TODO

      User(
        timeToTeachId = theTimeToTeachId,
        fullName = theFullName,
        givenName = theGivenName,
        familyName = theFamilyName,
        imageUrl = theImageUrl,
        emails = List(), // TODO
        userRole = theUserRole,
        schools = List() // TODO
      )
    }
  }

  override def extractAllScottishEsAndOs: Future[Seq[User]] = {
    val allUsersFutureDocuments: Future[Seq[Document]] = mongoFindQueriesProxy.findAllUsers

    allUsersFutureDocuments map {
      allUsersAsMongoDocuments =>
        val seqOfUsers: Seq[Try[User]] =
          for {
            singleUserDoc <- allUsersAsMongoDocuments
            singleUser: Try[User] = createUserFromMongoDocument(singleUserDoc)
          } yield singleUser

        val failures = seqOfUsers.filter(singleTry => singleTry.isFailure)

        if (failures.nonEmpty) {
          val errorMsg = "Failed to correctly parse users from database"
          log.error(errorMsg)
          return Future.failed(new RuntimeException(errorMsg))
        } else {
          seqOfUsers map { userTry => userTry.get }
        }
    }
  }
}
