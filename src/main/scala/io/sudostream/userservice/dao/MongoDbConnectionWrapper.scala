package io.sudostream.userservice.dao

import org.mongodb.scala.{Document, MongoCollection}

trait MongoDbConnectionWrapper {

  def getUsersCollection: MongoCollection[Document]

}
