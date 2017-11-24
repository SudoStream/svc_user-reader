package io.sudostream.userreader.dao

import io.sudostream.timetoteach.messages.systemwide.model.User
import io.sudostream.userreader.config.{ActorSystemWrapper, ConfigHelper}
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonString}
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
      users: Seq[User] =>
        assert(users.toList.size === 2)
    }
  }

  "Extract UserPreferences when not present" should "return None" in {
    val userReaderDao: MongoDbUserReaderDao = new MongoDbUserReaderDao(mongoFindQueries, actorSystemWrapper)
    assert(userReaderDao.extractUserPreferences(createNoBsonDocument()) === None)
  }

  "Extract UserPreferences when empty" should "return None" in {
    val userReaderDao: MongoDbUserReaderDao = new MongoDbUserReaderDao(mongoFindQueries, actorSystemWrapper)
    assert(userReaderDao.extractUserPreferences(createEmptyBsonDocument()) === None)
  }

  "Extract UserPreferences when has school times created" should "return User Preferences defined" in {
    val userReaderDao: MongoDbUserReaderDao = new MongoDbUserReaderDao(mongoFindQueries, actorSystemWrapper)
    val maybeUserPrefs = userReaderDao.extractUserPreferences(createValidUserPreferencesBsonDocument())
    assert(maybeUserPrefs.isDefined === true)
  }

  "Extract UserPreferences when has school times created" should "return User Preferences 1 element in school times" in {
    val userReaderDao: MongoDbUserReaderDao = new MongoDbUserReaderDao(mongoFindQueries, actorSystemWrapper)
    val maybeUserPrefs = userReaderDao.extractUserPreferences(createValidUserPreferencesBsonDocument())
    val userPreferences = maybeUserPrefs.get
    assert(userPreferences.allSchoolTimes.size === 1)
  }

  "Extract UserPreferences when has school times created" should "return User Preferences with a single school id of 'school1234'" in {
    val userReaderDao: MongoDbUserReaderDao = new MongoDbUserReaderDao(mongoFindQueries, actorSystemWrapper)
    val maybeUserPrefs = userReaderDao.extractUserPreferences(createValidUserPreferencesBsonDocument())
    val userPreferences = maybeUserPrefs.get
    assert(userPreferences.allSchoolTimes.head.schoolId === "school1234")
  }

  "Extract UserPreferences when has school times created" should "return User Preferences with a single school with 1 class instance " in {
    val userReaderDao: MongoDbUserReaderDao = new MongoDbUserReaderDao(mongoFindQueries, actorSystemWrapper)
    val maybeUserPrefs = userReaderDao.extractUserPreferences(createValidUserPreferencesBsonDocument())
    val userPreferences = maybeUserPrefs.get
    assert(userPreferences.allSchoolTimes.head.userTeachesTheseClasses.size === 1)
  }

  "Extract UserPreferences when has school times created" should "return User Preferences with a single school " +
    "with 1 class instance which has 2 curriculum levels " in {
    val userReaderDao: MongoDbUserReaderDao = new MongoDbUserReaderDao(mongoFindQueries, actorSystemWrapper)
    val maybeUserPrefs = userReaderDao.extractUserPreferences(createValidUserPreferencesBsonDocument())
    val userPreferences = maybeUserPrefs.get
    assert(userPreferences.allSchoolTimes.head.userTeachesTheseClasses.head.curriculumLevels.size === 2)
  }

  //////////////  Test Helper functions /////////////////
  private def createNoBsonDocument(): Option[BsonDocument] = None
  private def createEmptyBsonDocument(): Option[BsonDocument] = Some(BsonDocument())

  private def createValidUserPreferencesBsonDocument(): Option[BsonDocument] = {
    Some(BsonDocument(
      "allSchoolTimes" -> BsonArray(
        BsonDocument(
          "schoolId" -> "school1234",
          "schoolStartTime" -> "9:00 AM",
          "morningBreakStartTime" -> "10:30 AM",
          "morningBreakEndTime" -> "10:45 AM",
          "lunchStartTime" -> "12:00 PM",
          "lunchEndTime" -> "1:00 PM",
          "schoolEndTime" -> "3:00 PM",
          "userTeachesTheseClasses" -> BsonArray(
            BsonDocument(
              "className" -> "P1AB",
              "curriculumLevels" -> BsonArray(
                BsonDocument(
                  "curriculumLevel" -> BsonDocument(
                    "country" -> BsonString("SCOTLAND"),
                    "scottishCurriculumLevel" -> BsonString("EARLY")
                  )
                ),
                BsonDocument(
                  "curriculumLevel" -> BsonDocument(
                    "country" -> BsonString("SCOTLAND"),
                    "scottishCurriculumLevel" -> BsonString("FIRST")
                  )
                )
              )
            )
          )
        )
      )
    ))
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
