package io.sudostream.userreader.dao

import io.sudostream.timetoteach.messages.systemwide.model.SocialNetwork
import org.mongodb.scala.Document
import org.mongodb.scala.bson.BsonArray

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MongoFindQueriesProxyStub extends MongoFindQueriesProxy {

  private val userYvonne = Document(
    "_id" -> "someId124",
    "socialNetworkIds" -> BsonArray(
      Document(
        "socialNetwork" -> "GOOGLE",
        "id" -> "121212"
      )
    ),
    "fullName" -> "Yvonne Boyle",
    "givenName" -> "Yvonne",
    "familyName" -> "Boyle",
    "imageUrl" -> "https://some.url.com/here/is/picYve.jpg",
    "emails" -> BsonArray(
      Document(
        "emailAddress" -> "yvonne@sudostream.io",
        "validated" -> true,
        "preferred" -> true
      )
    ),
    "userRole" -> "TEACHER",
    "userAccountCreated" -> Document(
      "dateSignedUp_Iso8601" -> "2017-11-27",
      "timeSignedUp_Iso8601" -> "16:29:31.783"
    ),
    "schools" -> BsonArray(
      Document(
        "_id" -> "schoolId333",
        "name" -> "Some School",
        "address" -> "123 Some Place",
        "postCode" -> "AB10 100",
        "telephone" -> "123456789",
        "localAuthority" -> "SCOTLAND__CLACKMANNANSHIRE",
        "country" -> "SCOTLAND"
      )
    )
  )

  private val userAndy = Document(
    "_id" -> "someId123",
    "socialNetworkIds" -> BsonArray(
      Document(
        "socialNetwork" -> "GOOGLE",
        "id" -> "12345"
      )
    ),
    "fullName" -> "Andy Boyle",
    "givenName" -> "Andy",
    "familyName" -> "Boyle",
    "imageUrl" -> "https://some.url.com/here/is/pic.jpg",
    "emails" -> BsonArray(
      Document(
        "emailAddress" -> "andy@sudostream.io",
        "validated" -> true,
        "preferred" -> true
      )
    ),
    "userRole" -> "ADMIN",
    "userAccountCreated" -> Document(
      "dateSignedUp_Iso8601" -> "2017-11-27",
      "timeSignedUp_Iso8601" -> "16:29:31.783"
    ),
    "schools" -> BsonArray()
  )

  override def extractUserWithSocialIds(socialNetwork: SocialNetwork, socialNetworkId: String) = {
    if (socialNetwork == SocialNetwork.GOOGLE) {
      if (socialNetworkId == "12345") {
        Future {
          List(userAndy)
        }
      } else if (socialNetworkId == "121212") {
        Future {
          List(userYvonne)
        }
      } else {
        Future {
          List()
        }
      }
    }
    else {
      Future {
        List()
      }
    }
  }

  def findAllUsers: Future[Seq[Document]] = {


    Future {
      List(userAndy, userYvonne)
    }
  }

  override def extractUserWithTimeToTeachUserId(timeToTeachUserId: String): Future[Seq[Document]] = {
    Future {
      List(userAndy, userYvonne)
    }
  }
}
