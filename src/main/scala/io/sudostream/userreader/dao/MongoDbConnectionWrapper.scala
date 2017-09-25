package io.sudostream.userreader.dao

import org.mongodb.scala.{Document, MongoCollection}

trait MongoDbConnectionWrapper {

  def getUsersCollection: MongoCollection[Document]

}
