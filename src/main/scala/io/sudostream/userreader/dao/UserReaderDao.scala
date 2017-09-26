package io.sudostream.userreader.dao

import io.sudostream.timetoteach.messages.systemwide.model.{SocialNetwork, User}

import scala.concurrent.Future

trait UserReaderDao {

  def extractAllUsers: Future[Seq[User]]

  def extractUserWithSocialIds(socialNetwork: SocialNetwork, socialNetworkId: String): Future[Seq[User]]
}
