package io.sudostream.userreader.dao

import java.util

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.Materializer
import io.sudostream.timetoteach.messages.scottish.ScottishCurriculumLevel
import io.sudostream.timetoteach.messages.systemwide.model._
import io.sudostream.userreader.config.ActorSystemWrapper
import org.bson.BsonValue
import org.mongodb.scala.Document
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDocument, BsonString}

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try

sealed class MongoDbUserReaderDao(mongoFindQueriesProxy: MongoFindQueriesProxy,
                                  actorSystemWrapper: ActorSystemWrapper) extends UserReaderDao {

  implicit val system: ActorSystem = actorSystemWrapper.system
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = actorSystemWrapper.materializer
  val log: LoggingAdapter = system.log


  def createUserFromMongoDocument(singleUserDoc: Document): Try[User] = {
    Try {

      val theTimeToTeachId: String = singleUserDoc.getString("_id")
      val theSocialNetworkIds: List[SocialNetworkIdWrapper] = extractSocialNetworkIds(singleUserDoc.get[BsonArray]("socialNetworkIds"))
      val theFullName: String = singleUserDoc.getString("fullName")
      val theGivenName: Option[String] = singleUserDoc.getString("givenName") match {
        case str: String => if (str == null || str.isEmpty) Option.empty else Some(str)
        case _ => Option.empty
      }
      val theFamilyName: Option[String] = singleUserDoc.getString("familyName") match {
        case str: String => if (str == null || str.isEmpty) Option.empty else Some(str)
        case _ => Option.empty
      }
      val theImageUrl: Option[String] = singleUserDoc.getString("imageUrl") match {
        case str: String => if (str == null || str.isEmpty) Option.empty else Some(str)
        case _ => Option.empty
      }
      val emailDetails: List[EmailDetails] = extractEmailDetails(singleUserDoc.get[BsonArray]("emails"))

      val theUserRole: UserRole = singleUserDoc.getString("userRole") match {
        case "TEACHER" => UserRole.TEACHER
        case "ADMIN" => UserRole.ADMIN
        case _ => UserRole.OTHER
      }

      val theUserSignedUpDetails = singleUserDoc.get[BsonDocument]("userAccountCreated").getOrElse(
        BsonDocument(
          "dateSignedUp_Iso8601" -> "2017-01-01",
          "timeSignedUp_Iso8601" -> "01:00"
        )
      )

      val theSchools: List[SchoolWrapper] = extractSchools(singleUserDoc.get[BsonArray]("schools"))

      val theUserPreferences: Option[UserPreferences] = extractUserPreferences(singleUserDoc.get[BsonDocument]("userPreferences"))

      User(
        timeToTeachId = theTimeToTeachId,
        socialNetworkIds = theSocialNetworkIds,
        fullName = theFullName,
        givenName = theGivenName,
        familyName = theFamilyName,
        imageUrl = theImageUrl,
        emails = emailDetails,
        userRole = theUserRole,
        userAccountCreated = UserAccountCreatedDetails(
          dateSignedUp_Iso8601 = theUserSignedUpDetails.getString("dateSignedUp_Iso8601").getValue,
          timeSignedUp_Iso8601 = theUserSignedUpDetails.getString("timeSignedUp_Iso8601").getValue
        ),
        schools = theSchools,
        userPreferences = theUserPreferences
      )
    }
  }

  private def createUserFromDocuments(userDocuments: Future[Seq[Document]]): Future[Seq[User]] = {
    userDocuments map {
      allUsersAsMongoDocuments =>
        val seqOfUsers: Seq[Try[User]] =
          for {
            singleUserDoc <- allUsersAsMongoDocuments
            singleUser: Try[User] = createUserFromMongoDocument(singleUserDoc)
          } yield singleUser

        val failures = seqOfUsers.filter(singleTry => singleTry.isFailure)

        if (failures.nonEmpty) {
          failures foreach (fail => log.error(s"problem with $fail"))
          val errorMsg = "Failed to correctly parse users from database"
          log.error(errorMsg)
          return Future.failed(new RuntimeException(errorMsg))
        } else {
          seqOfUsers map { userTry => userTry.get }
        }
    }
  }

  override def extractAllUsers: Future[Seq[User]] = {
    val allUsersFutureDocuments: Future[Seq[Document]] = mongoFindQueriesProxy.findAllUsers
    createUserFromDocuments(allUsersFutureDocuments)
  }

  override def extractUserWithSocialIds(socialNetwork: SocialNetwork, socialNetworkId: String): Future[Seq[User]] = {
    val userWithSocialIdsFutureDocuments: Future[Seq[Document]] =
      mongoFindQueriesProxy.extractUserWithSocialIds(socialNetwork, socialNetworkId)

    createUserFromDocuments(userWithSocialIdsFutureDocuments)
  }

  override def extractUserWithTimeToTeachUserId(timeToTeachUserId: String): Future[Seq[User]] = {
    val userWithSocialIdsFutureDocuments: Future[Seq[Document]] =
      mongoFindQueriesProxy.extractUserWithTimeToTeachUserId(timeToTeachUserId)

    createUserFromDocuments(userWithSocialIdsFutureDocuments)
  }

  def extractCurriculumLevels(curriculumLevelsAsBsonArray: BsonArray): List[CurriculumLevelWrapper] = {
    {
      for {
        curriculumLevelWrapperAsBsonValue <- curriculumLevelsAsBsonArray
        curriculumLevelWrapperAsBsonDoc = curriculumLevelWrapperAsBsonValue.asInstanceOf[BsonDocument]
        curriculumLevelAsBsonDoc = curriculumLevelWrapperAsBsonDoc.getDocument("curriculumLevel")
        country = curriculumLevelAsBsonDoc.getString("country").getValue match {
          case "EIRE" => Country.EIRE
          case "ENGLAND" => Country.ENGLAND
          case "NORTHERN_IRELAND" => Country.NORTHERN_IRELAND
          case "SCOTLAND" => Country.SCOTLAND
          case "WALES" => Country.WALES
          case other: String => Country.UNKNOWN
        }
        scottishCurriculumLevel = curriculumLevelAsBsonDoc.getString("scottishCurriculumLevel").getValue match {
          case "EARLY" => Some(ScottishCurriculumLevel.EARLY)
          case "FIRST" => Some(ScottishCurriculumLevel.FIRST)
          case "SECOND" => Some(ScottishCurriculumLevel.SECOND)
          case "THIRD" => Some(ScottishCurriculumLevel.THIRD)
          case "FOURTH" => Some(ScottishCurriculumLevel.FOURTH)
          case _ => None
        }
      } yield CurriculumLevelWrapper(
        curriculumLevel = CurriculumLevel(
          country = country,
          scottishCurriculumLevel = scottishCurriculumLevel
        )
      )
    }.toList
  }

  def extractSchoolClassesForSchool(schoolTimesAsBsonDoc: BsonDocument): List[SchoolClass] = {
    val schoolClassesAsBsonArray = schoolTimesAsBsonDoc.getArray("userTeachesTheseClasses")

    val schoolClasses = for {
      taughtClassAsBsonValue <- schoolClassesAsBsonArray
      taughtClassAsBsonDoc = taughtClassAsBsonValue.asInstanceOf[BsonDocument]
      className: String = taughtClassAsBsonDoc.getString("className").getValue
      curriculumLevels: List[CurriculumLevelWrapper] = extractCurriculumLevels(taughtClassAsBsonDoc.getArray("curriculumLevels"))
    } yield SchoolClass(
      className = className,
      curriculumLevels = curriculumLevels)

    schoolClasses.toList
  }

  private[dao] def extractUserPreferences(maybeUserPreferences: Option[BsonDocument]): Option[UserPreferences] = {
    maybeUserPreferences match {
      case Some(userPrefs) =>
        if (userPrefs.isEmpty) None
        else {
          val allSchoolTimesMongoArray = userPrefs.getArray("allSchoolTimes")

          val schoolTimesSequence = {
            for {
              schoolTimesBsonValue <- allSchoolTimesMongoArray
              schoolTimesAsBsonDoc = schoolTimesBsonValue.asInstanceOf[BsonDocument]
              allClassesForSchool: List[SchoolClass] = extractSchoolClassesForSchool(schoolTimesAsBsonDoc)

              schoolTimes = SchoolTimes(
                schoolId = schoolTimesAsBsonDoc.getString("schoolId").getValue,
                schoolStartTime = schoolTimesAsBsonDoc.getString("schoolStartTime").getValue,
                morningBreakStartTime = schoolTimesAsBsonDoc.getString("morningBreakStartTime").getValue,
                morningBreakEndTime = schoolTimesAsBsonDoc.getString("morningBreakEndTime").getValue,
                lunchStartTime = schoolTimesAsBsonDoc.getString("lunchStartTime").getValue,
                lunchEndTime = schoolTimesAsBsonDoc.getString("lunchEndTime").getValue,
                schoolEndTime = schoolTimesAsBsonDoc.getString("schoolEndTime").getValue,
                userTeachesTheseClasses = allClassesForSchool
              )
            } yield schoolTimes
          }.toList

          Some(UserPreferences(allSchoolTimes = schoolTimesSequence))
        }
      case None => None
    }
  }

  private[dao] def extractSocialNetworkIds(maybeSocialNetworkIds: Option[BsonArray]): List[SocialNetworkIdWrapper] = {
    maybeSocialNetworkIds match {
      case Some(socialNetworkIdsAsBsonArray) =>
        println(s"is it a bson array: ${socialNetworkIdsAsBsonArray.toString}")
        val socialNetworkIdsAsBsonList = socialNetworkIdsAsBsonArray.toList

        val socialNetworkIdsDetailTupleSeq = for {
          socialIdElem <- socialNetworkIdsAsBsonList
          socialIdDoc = socialIdElem.asDocument()

          socialNetworkName: BsonString = socialIdDoc.getString("socialNetwork")
          socialNetworkUserId: BsonString = socialIdDoc.getString("id")
        } yield (socialNetworkName.getValue, socialNetworkUserId.getValue)

        {
          socialNetworkIdsDetailTupleSeq map {
            socialNetworkIdTuple =>

              val socialNetworkName = socialNetworkIdTuple._1 match {
                case "FACEBOOK" => SocialNetwork.FACEBOOK
                case "GOOGLE" => SocialNetwork.GOOGLE
                case "TWITTER" => SocialNetwork.TWITTER
                case _ => SocialNetwork.OTHER
              }

              SocialNetworkIdWrapper(
                socialNetworkId = SocialNetworkId(
                  socialNetwork = socialNetworkName,
                  id = socialNetworkIdTuple._2
                )
              )
          }
        }.toList

      case None => throwRuntimeError("socialNetworkIds")
    }

  }

  private[dao] def extractEmailDetails(emailsAsBsonArrayOption: Option[BsonArray]): List[EmailDetails] = {
    emailsAsBsonArrayOption match {
      case Some(emailsAsBsonArray) =>
        val emailsAsBsonValuesList: util.List[BsonValue] = emailsAsBsonArray.getValues

        val emailDetailsTupleSeq: Seq[(String, Boolean, Boolean)] = for {
          emailDetailElem: BsonValue <- emailsAsBsonValuesList
          emailDetailDoc = emailDetailElem.asDocument()

          emailAddressBson: BsonString = emailDetailDoc.getString("emailAddress")
          emailValidatedBson: BsonBoolean = emailDetailDoc.getBoolean("validated")
          emailPreferredBson: BsonBoolean = emailDetailDoc.getBoolean("preferred")
        } yield (emailAddressBson.getValue, emailValidatedBson.getValue, emailPreferredBson.getValue)

        {
          emailDetailsTupleSeq map {
            emailTuple =>
              EmailDetails(
                emailAddress = emailTuple._1,
                validated = emailTuple._2,
                preferred = emailTuple._3
              )
          }
        }.toList

      case None => throwRuntimeError("emails")
    }
  }

  private[dao] def extractSchools(schoolsAsBsonArrayOption: Option[BsonArray]): List[SchoolWrapper] = {
    schoolsAsBsonArrayOption match {
      case Some(schoolsBsonArray) =>
        val schoolsAsBsonValuesList: util.List[BsonValue] = schoolsBsonArray.getValues

        val schoolTupleSeq: Seq[(String, String, String, String, String, String, String)]
        = for {
          schoolDetailElem: BsonValue <- schoolsAsBsonValuesList
          schoolDetailDoc = schoolDetailElem.asDocument()

          schoolId: BsonString = schoolDetailDoc.getString("_id")
          schoolName: BsonString = schoolDetailDoc.getString("name")
          schoolAddress: BsonString = schoolDetailDoc.getString("address")
          schoolPostCode: BsonString = schoolDetailDoc.getString("postCode")
          schoolTelephone: BsonString = schoolDetailDoc.getString("telephone")
          schoolLocalAuthority: BsonString = schoolDetailDoc.getString("localAuthority")
          schoolCountry: BsonString = schoolDetailDoc.getString("country")

        } yield (
          schoolId.getValue,
          schoolName.getValue,
          schoolAddress.getValue,
          schoolPostCode.getValue,
          schoolTelephone.getValue,
          schoolLocalAuthority.getValue,
          schoolCountry.getValue
        )


        val schools = schoolTupleSeq map {
          schoolTuple =>
            val localAuthority: LocalAuthority = schoolTuple._6 match {
              case "SCOTLAND__GRANT_MAINTAINED" => LocalAuthority.SCOTLAND__GRANT_MAINTAINED
              case "SCOTLAND__ABERDEEN_CITY" => LocalAuthority.SCOTLAND__ABERDEEN_CITY
              case "SCOTLAND__ABERDEENSHIRE" => LocalAuthority.SCOTLAND__ABERDEENSHIRE
              case "SCOTLAND__ANGUS" => LocalAuthority.SCOTLAND__ANGUS
              case "SCOTLAND__ARGYLL_AND_BUTE" => LocalAuthority.SCOTLAND__ARGYLL_AND_BUTE
              case "SCOTLAND__COMHAIRLE_NAN_EILEAN_SIAR" => LocalAuthority.SCOTLAND__COMHAIRLE_NAN_EILEAN_SIAR
              case "SCOTLAND__CLACKMANNANSHIRE" => LocalAuthority.SCOTLAND__CLACKMANNANSHIRE
              case "SCOTLAND__DUMFRIES_AND_GALLOWAY" => LocalAuthority.SCOTLAND__DUMFRIES_AND_GALLOWAY
              case "SCOTLAND__DUNDEE_CITY" => LocalAuthority.SCOTLAND__DUNDEE_CITY
              case "SCOTLAND__EAST_AYRSHIRE" => LocalAuthority.SCOTLAND__EAST_AYRSHIRE
              case "SCOTLAND__EAST_DUMBARTONSHIRE" => LocalAuthority.SCOTLAND__EAST_DUMBARTONSHIRE
              case "SCOTLAND__EDINBURGH_CITY" => LocalAuthority.SCOTLAND__EDINBURGH_CITY
              case "SCOTLAND__EAST_LOTHIAN" => LocalAuthority.SCOTLAND__EAST_LOTHIAN
              case "SCOTLAND__EAST_RENFREWSHIRE" => LocalAuthority.SCOTLAND__EAST_RENFREWSHIRE
              case "SCOTLAND__FALKIRK" => LocalAuthority.SCOTLAND__FALKIRK
              case "SCOTLAND__FIFE" => LocalAuthority.SCOTLAND__FIFE
              case "SCOTLAND__GLASGOW" => LocalAuthority.SCOTLAND__GLASGOW
              case "SCOTLAND__HIGHLAND" => LocalAuthority.SCOTLAND__HIGHLAND
              case "SCOTLAND__INVERCLYDE" => LocalAuthority.SCOTLAND__INVERCLYDE
              case "SCOTLAND__MIDLOTHIAN" => LocalAuthority.SCOTLAND__MIDLOTHIAN
              case "SCOTLAND__MORAY" => LocalAuthority.SCOTLAND__MORAY
              case "SCOTLAND__NORTH_AYRSHIRE" => LocalAuthority.SCOTLAND__NORTH_AYRSHIRE
              case "SCOTLAND__NORTH_LANARKSHIRE" => LocalAuthority.SCOTLAND__NORTH_LANARKSHIRE
              case "SCOTLAND__ORKNEY" => LocalAuthority.SCOTLAND__ORKNEY
              case "SCOTLAND__PERTH_AND_KINROSS" => LocalAuthority.SCOTLAND__PERTH_AND_KINROSS
              case "SCOTLAND__RENFREWSHIRE" => LocalAuthority.SCOTLAND__RENFREWSHIRE
              case "SCOTLAND__SCOTTISH_BORDERS" => LocalAuthority.SCOTLAND__SCOTTISH_BORDERS
              case "SCOTLAND__SHETLAND_ISLANDS" => LocalAuthority.SCOTLAND__SHETLAND_ISLANDS
              case "SCOTLAND__SOUTH_AYRSHIRE" => LocalAuthority.SCOTLAND__SOUTH_AYRSHIRE
              case "SCOTLAND__SOUTH_LANARKSHIRE" => LocalAuthority.SCOTLAND__SOUTH_LANARKSHIRE
              case "SCOTLAND__STIRLING" => LocalAuthority.SCOTLAND__STIRLING
              case "SCOTLAND__WEST_DUMBARTONSHIRE" => LocalAuthority.SCOTLAND__WEST_DUMBARTONSHIRE
              case "SCOTLAND__WEST_LOTHIAN" => LocalAuthority.SCOTLAND__WEST_LOTHIAN

              case other: String => throwRuntimeError(s"LocalAuthority '$other' not recognised")
            }

            val country: Country = schoolTuple._7 match {
              case "EIRE" => Country.EIRE
              case "ENGLAND" => Country.ENGLAND
              case "NORTHERN_IRELAND" => Country.NORTHERN_IRELAND
              case "SCOTLAND" => Country.SCOTLAND
              case "WALES" => Country.WALES
              case other: String => throwRuntimeError(s"Country '$other' not recognised")
            }

            School(
              id = schoolTuple._1,
              name = schoolTuple._2,
              address = schoolTuple._3,
              postCode = schoolTuple._4,
              telephone = schoolTuple._5,
              localAuthority = localAuthority,
              country = country
            )
        }

        {
          schools map { school => SchoolWrapper(school) }
        }.toList


      case None => throwRuntimeError("schools")
    }
  }

  private def throwRuntimeError(parseContext: String) = {
    val errorMsg = s"Failed to correctly $parseContext from database"
    log.error(errorMsg)
    throw new RuntimeException(errorMsg)
  }

}