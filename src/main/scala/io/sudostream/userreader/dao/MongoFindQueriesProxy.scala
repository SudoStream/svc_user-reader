package io.sudostream.userreader.dao

import io.sudostream.timetoteach.messages.systemwide.model.SocialNetwork
import org.mongodb.scala.Document

import scala.concurrent.Future

trait MongoFindQueriesProxy {

  def findAllUsers: Future[Seq[Document]]

  def extractUserWithSocialIds(socialNetwork: SocialNetwork, socialNetworkId: String): Future[Seq[Document]]
}
