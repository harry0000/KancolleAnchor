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

  def update(uid: String) = MaintenanceApiAction { Action(BodyParsers.parse.json) { req =>
    req.body.validate[Admiral.ForRest] match {
      case e: JsError =>
        BadRequest(JsonParseError(e).toJson)
      case JsSuccess(admiral, _) if uid != admiral.admiralId =>
        BadRequest
      case JsSuccess(admiral, _) =>
        DB localTx { implicit session =>
          AdmiralDao.find(admiral).map {
            AdmiralDao.updateName(_, admiral.name)
          } match {
            case Some(1) => Ok
            case _ => BadRequest
          }
        }
    }
  }}

  def delete(uid: String, key: Option[String]) = MaintenanceApiAction { Action {
    NotImplemented
  }}

}
