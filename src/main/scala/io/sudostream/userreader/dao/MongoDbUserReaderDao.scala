package io.sudostream.userreader.dao

import java.util

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.Materializer
import io.sudostream.timetoteach.messages.systemwide.model._
import io.sudostream.userreader.config.ActorSystemWrapper
import org.bson.BsonValue
import org.mongodb.scala.Document
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonString}

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

      val theSchools: List[SchoolWrapper] = extractSchools(singleUserDoc.get[BsonArray]("schools"))

      User(
        timeToTeachId = theTimeToTeachId,
        socialNetworkIds = theSocialNetworkIds,
        fullName = theFullName,
        givenName = theGivenName,
        familyName = theFamilyName,
        imageUrl = theImageUrl,
        emails = emailDetails,
        userRole = theUserRole,
        schools = theSchools
      )
    }
  }

  private def createUserFromDocuments( userDocuments: Future[Seq[Document]]) : Future[Seq[User]] = {
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
    val userWithSocialIdsFutureDocuments: Future[Seq[Document]] = mongoFindQueriesProxy.extractUserWithSocialIds(socialNetwork, socialNetworkId)
    createUserFromDocuments(userWithSocialIdsFutureDocuments)
  }

  private[dao] def extractSocialNetworkIds(maybeSocialNetworkIds: Option[BsonArray]): List[SocialNetworkIdWrapper] = {
    maybeSocialNetworkIds match {
      case Some(socialNetworkIdsAsBsonArray) =>
        val socialNetworkIdsAsBsonList: util.List[BsonValue] = socialNetworkIdsAsBsonArray.getValues

        val socialNetworkIdsDetailTupleSeq: Seq[(String, String)] = for {
          socialIdElem: BsonValue <- socialNetworkIdsAsBsonList
          socialIdDoc = socialIdElem.asDocument()

          socialNetworkName: BsonString = socialIdDoc.getString("socialNetworkName")
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
              case "SCOTLAND__ABERDEEN_CITY" => LocalAuthority.SCOTLAND__ABERDEEN_CITY
              case "SCOTLAND__ABERDEENSHIRE" => LocalAuthority.SCOTLAND__ABERDEENSHIRE
              case "SCOTLAND__ANGUS" => LocalAuthority.SCOTLAND__ANGUS
              case "SCOTLAND__ARGYLL_AND_BUTE" => LocalAuthority.SCOTLAND__ARGYLL_AND_BUTE
              case "SCOTLAND__EDINBURGH_CITY" => LocalAuthority.SCOTLAND__EDINBURGH_CITY
              case "SCOTLAND__CLACKMANNANSHIRE" => LocalAuthority.SCOTLAND__CLACKMANNANSHIRE
              case "SCOTLAND__DUMFRIES_AND_GALLOWAY" => LocalAuthority.SCOTLAND__DUMFRIES_AND_GALLOWAY
              case "SCOTLAND__DUNDEE_CITY" => LocalAuthority.SCOTLAND__DUNDEE_CITY
              case "SCOTLAND__EAST_AYRSHIRE" => LocalAuthority.SCOTLAND__EAST_AYRSHIRE
              case "SCOTLAND__STIRLING" => LocalAuthority.SCOTLAND__STIRLING
              case "SCOTLAND__GLASGOW" => LocalAuthority.SCOTLAND__GLASGOW
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