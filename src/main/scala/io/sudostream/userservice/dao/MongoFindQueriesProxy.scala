package io.sudostream.userservice.dao

import org.mongodb.scala.Document

import scala.concurrent.Future

trait MongoFindQueriesProxy {
  def findAllUsers : Future[Seq[Document]]
}