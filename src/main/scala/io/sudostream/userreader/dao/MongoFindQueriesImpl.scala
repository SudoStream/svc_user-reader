package io.sudostream.userreader.dao

import io.sudostream.timetoteach.messages.systemwide.model.SocialNetwork
import org.mongodb.scala.bson.{BsonArray, BsonString}
import org.mongodb.scala.{Document, FindObservable, MongoCollection}

import scala.concurrent.Future

class MongoFindQueriesImpl(mongoDbConnectionWrapper: MongoDbConnectionWrapper) extends MongoFindQueriesProxy {

  val usersCollection: MongoCollection[Document] = mongoDbConnectionWrapper.getUsersCollection

  override def findAllUsers: Future[Seq[Document]] = {
    val usersMongoDocuments: FindObservable[Document] = usersCollection.find(Document())
    usersMongoDocuments.toFuture()
  }

  override def
  extractUserWithSocialIds(socialNetwork: SocialNetwork, socialNetworkId: String) : Future[Seq[Document]] = {
    val findMatcher = Document(
      "socialNetworkIds" -> BsonArray(
        Document(
        "socialNetworkName" -> BsonString(socialNetwork.toString.toUpperCase),
        "id" -> BsonString(socialNetworkId)
        )
      )
    )

    val usersMongoDocuments: FindObservable[Document] = usersCollection.find(findMatcher)
    usersMongoDocuments.toFuture()
  }
}
