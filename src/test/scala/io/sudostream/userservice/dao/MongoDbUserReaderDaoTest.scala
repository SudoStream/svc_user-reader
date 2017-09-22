package io.sudostream.userservice.dao

import io.sudostream.timetoteach.messages.systemwide.model.{Country, User}
import io.sudostream.userservice.config.{ActorSystemWrapper, ConfigHelper}
import org.scalatest.AsyncFlatSpec
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

class MongoDbUserReaderDaoTest extends AsyncFlatSpec with MockitoSugar {

  private val configHelper = new ConfigHelper
  private val actorSystemWrapper = new ActorSystemWrapper(configHelper)
  private val mongoFindQueries = new MongoFindQueriesProxyStub

  "Extracting All Users from Dao" should "return list of size 2 users" in {
    val userReaderDao: UserReaderDao = new MongoDbUserReaderDao(mongoFindQueries, actorSystemWrapper)
    val allUsersFuture: Future[Seq[User]] = userReaderDao.extractAllUsers

    allUsersFuture map {
      users : Seq[User] =>
        assert(users.toList.size === 2 )
    }
  }

//  "Extracting All Users from Dao" should "return list with one Scotland Local Authority and one England" in {
//    val userReaderDao: UserReaderDao = new MongoDbUserReaderDao(mongoFindQueries, actorSystemWrapper)
//    val allUsersFuture: Future[Seq[User]] = userReaderDao.extractAllUsers
//
//    allUsersFuture map {
//      users : Seq[User] =>
//        assert(users.toList.filter( user => user.schools.count(schoolWrapper => schoolWrapper.school.country == Country.SCOTLAND) == 1 ))
//    }
//  }


}
