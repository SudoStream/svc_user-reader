package io.sudostream.userreader.dao.mongo

import org.mongodb.scala.{Document, MongoCollection}

trait MongoDbConnectionWrapper {

  def getUsersCollection: MongoCollection[Document]

}
