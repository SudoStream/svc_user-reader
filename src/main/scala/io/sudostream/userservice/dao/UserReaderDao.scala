package io.sudostream.userservice.dao

import io.sudostream.timetoteach.messages.systemwide.model.User

import scala.concurrent.Future

trait UserReaderDao {
  def extractAllScottishEsAndOs: Future[Seq[User]]
}
