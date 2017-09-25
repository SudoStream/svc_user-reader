package io.sudostream.userreader.dao

import org.mongodb.scala.Document
import org.mongodb.scala.bson.BsonArray

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MongoFindQueriesProxyStub extends MongoFindQueriesProxy {

  def findAllUsers: Future[Seq[Document]] = {
    val userAndy = Document(
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
      "schools" -> BsonArray()
    )

    val userYvonne = Document(
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

    Future {
      List(userAndy, userYvonne)
    }
  }

}
