package io.sudostream.userservice.dao

import org.mongodb.scala.{Document, FindObservable, MongoCollection}

import scala.concurrent.Future

class MongoFindQueriesImpl(mongoDbConnectionWrapper: MongoDbConnectionWrapper) extends MongoFindQueriesProxy {

  val esAndOsCollection: MongoCollection[Document] = mongoDbConnectionWrapper.getUsersCollection

  def findAllUsers: Future[Seq[Document]] = {
    val esAndOsMongoDocuments: FindObservable[Document] = esAndOsCollection.find(Document())
    esAndOsMongoDocuments.toFuture()
  }

}
