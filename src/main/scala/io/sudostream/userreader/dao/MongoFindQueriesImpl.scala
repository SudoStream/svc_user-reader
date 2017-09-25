package io.sudostream.userreader.dao

import org.mongodb.scala.{Document, FindObservable, MongoCollection}

import scala.concurrent.Future

class MongoFindQueriesImpl(mongoDbConnectionWrapper: MongoDbConnectionWrapper) extends MongoFindQueriesProxy {

  val usersCollection: MongoCollection[Document] = mongoDbConnectionWrapper.getUsersCollection

  def findAllUsers: Future[Seq[Document]] = {
    val esAndOsMongoDocuments: FindObservable[Document] = usersCollection.find(Document())
    esAndOsMongoDocuments.toFuture()
  }

}
