package controllers

import javax.inject.{Inject, Singleton}

import arcade.Errors._
import arcade.{Config, DamageLevel, MaintenanceApi, MaintenanceController}
import models.dao.{AdmiralDao, AnchorDao, SpottingDao}
import models.dto.Spotting
import play.api.libs.json.Json
import play.api.mvc.{Action, BodyParsers, Controller}
import play.api.{Configuration, Logger}
import scalikejdbc.DB
import time.{ClockProvider, Duration, UTCDateTime}

@Singleton
class Spottings @Inject() (val configuration: Configuration,
                           implicit val clockProvider: ClockProvider) extends Controller with MaintenanceController with MaintenanceApi {

  import models.JsonConverter._

  val logger = Logger(this.getClass)
  val config = Config(configuration)

  def list(prefecture: Int, place: Int, uid: Option[String], created_at: Option[Long]) = MaintenanceApiAction { Action { req =>
    DB readOnly { implicit session =>
      (for {
        id <- uid
        created <- created_at
        admiral <- AdmiralDao.find(id, created)
      } yield {
        Ok(Json.toJson(SpottingDao.list(prefecture, place, admiral)))
      }) match {
        case Some(r) => r
        case _ => BadRequest
      }
    }
  }}

  def create(created_at: Option[Long]) = MaintenanceApiAction { Action(BodyParsers.parse.json) { req =>
    DB localTx { implicit session =>
      (for {
        c        <- created_at.toRight(BadRequest).right
        spotting <- req.body.validate[Spotting.ForRest].asEither.left.map(e => BadRequest(JsonParseError(e).toJson)).right
        spotter  <- AdmiralDao.find(spotting.spotter, c).toRight(BadRequest).right
        summary  <- AnchorDao.summarySpotting(spotting, spotter).toRight(BadRequest(AnchorNotFound.toJson)).right
      } yield {
        if (summary.reportedCount >= DamageLevel.Sank.value) {
          Forbidden(AnchorAlreadySunk.toJson)
        } else if (summary.lastReported.map(Duration(_, UTCDateTime()).toMillis).exists(_ < config.leastSpottingInterval)) {
          Forbidden(TooShortSpottingInterval.toJson)
        } else {
          try {
            val created = SpottingDao.create(spotting, spotter, UTCDateTime())
            Created(Json.toJson(created)).withHeaders(LOCATION -> ("/spottings" + created.location))
          } catch { case scala.util.control.NonFatal(e) =>
            logger.error("Error while creating spotting", e)
            Conflict(AnchorAlreadySpotted.toJson)
          }
        }
      }) match {
        case Right(r) => r
        case Left(r) => r
      }
    }
  }}

}
