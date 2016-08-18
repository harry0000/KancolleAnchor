package controllers

import javax.inject.{Inject, Singleton}

import arcade.Errors._
import arcade.{MaintenanceApi, MaintenanceController}
import models.dao.AdmiralDao
import models.dto.Admiral
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc._
import scalikejdbc.DB
import time.{ClockProvider, UTCDateTime}

@Singleton
class Admirals @Inject() (val configuration: Configuration,
                          implicit val clockProvider: ClockProvider) extends Controller with MaintenanceController with MaintenanceApi {

  import models.JsonConverter._

  def create = MaintenanceApiAction { Action {
    DB autoCommit { implicit session =>
      try {
        val admiral = AdmiralDao.create(UTCDateTime())
        Created(Json.toJson(admiral)).withHeaders(LOCATION -> ("/admirals" + admiral.location))
      } catch { case scala.util.control.NonFatal(e) =>
        // TODO: logging
        e.printStackTrace()
        Conflict(DuplicateAdmiral.toJson)
      }
    }
  }}

  def update(uid: String, created_at: Option[Long]) = MaintenanceApiAction { Action(BodyParsers.parse.json) { req =>
    (for {
      c    <- created_at.toRight(BadRequest).right
      name <- req.body.validate((JsPath \ 'name).readNullable[String](validAdmiralName))
                      .asEither.left.map(e => BadRequest(JsonParseError(e).toJson)).right
    } yield {
      DB localTx { implicit session =>
        AdmiralDao.find(uid, c).map {
          AdmiralDao.updateName(_, name)
        } match {
          case Some(1) => Ok
          case _       => BadRequest
        }
      }
    }) match {
      case Right(r) => r
      case Left(r)  => r
    }
  }}

  def delete(uid: String, key: Option[String]) = MaintenanceApiAction { Action {
    NotImplemented
  }}

}
