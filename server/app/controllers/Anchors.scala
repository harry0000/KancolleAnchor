package controllers

import javax.inject.{Inject, Singleton}

import arcade.Errors._
import arcade.{MaintenanceApi, MaintenanceController}
import models.dao.{AdmiralDao, AnchorDao}
import models.dto.Anchor
import play.api.Configuration
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc._
import scalikejdbc.DB
import time.{ClockProvider, UTCDateTime}

@Singleton
class Anchors @Inject() (val configuration: Configuration,
                         implicit val clockProvider: ClockProvider) extends Controller with MaintenanceController with MaintenanceApi {

  import models.JsonConverter._

  private case class Position(
    page: Int,
    number: Int
  )

  private case class AnchorValues(
    position: Option[Position],
    weighed: Option[Long]
  )

  private implicit val updateValueReads = new Reads[AnchorValues] {
    override def reads(json: JsValue): JsResult[AnchorValues] = {
      ((json \ "page"   ).validateOpt[Int](min(1)) and
       (json \ "number" ).validateOpt[Int](min(1)) and
       (json \ "weighed").validateOpt[Long](min(0L))
      ).tupled match {
        case e: JsError => e
        case JsSuccess(v, _) =>
          v match {
            case (None,    Some(_), _      ) => JsError("\"page\" is required")
            case (Some(_), None,    _      ) => JsError("\"number\" is required")
            case (Some(_), Some(_), Some(_)) => JsError("Cannot update \"page\", \"number\" and \"weighed\" at the same time")
            case (Some(p), Some(n), _      ) => JsSuccess(AnchorValues(Some(Position(p, n)), None))
            case (None,    None,    w      ) => JsSuccess(AnchorValues(None, w))
          }
      }
    }
  }

  def list(prefecture: Int, place: Int, credits: Int) = MaintenanceApiAction { Action {
    DB readOnly { implicit session =>
      Ok(Json.toJson(AnchorDao.list(prefecture, place, credits)))
    }
  }}

  def create(created_at: Option[Long]) = MaintenanceApiAction { Action(BodyParsers.parse.json) { req =>
    DB localTx { implicit session =>
      (for {
        c       <- created_at.toRight(BadRequest).right
        anchor  <- req.body.validate[Anchor.ForRest].asEither.left.map(e => BadRequest(JsonParseError(e).toJson)).right
        admiral <- AdmiralDao.find(anchor.admiral, c).toRight(BadRequest).right
      } yield {
        try {
          val created = AnchorDao.create(anchor, admiral, UTCDateTime())
          Created(Json.toJson(created)).withHeaders(LOCATION -> ("/anchors" + created.location))
        } catch { case scala.util.control.NonFatal(e) =>
          // TODO: logging
          e.printStackTrace()
          Conflict(DuplicateAnchor.toJson)
        }
      }) match {
        case Right(r) => r
        case Left(r) => r
      }
    }
  }}

  def update(prefecture: Int, place: Int, credits: Int, uuid: String, anchored: Long, created_at: Option[Long]) = MaintenanceApiAction { Action(BodyParsers.parse.json) { req =>
    DB localTx { implicit session =>
      (for {
        c       <- created_at.toRight(BadRequest).right
        admiral <- AdmiralDao.find(uuid, c).toRight(BadRequest).right
        anchor  <- AnchorDao.find(prefecture, place, credits, anchored, admiral)
                            .toRight(NotFound(AnchorNotFound.toJson)).right
        values  <- req.body.validate[AnchorValues]
                           .asEither.left.map(e => BadRequest(JsonParseError(e).toJson)).right
      } yield {
        values match {
          case AnchorValues(Some(position), _) =>
            AnchorDao.updatePosition(anchor, position.page, position.number) match {
              case 1 => Ok
              case _ => Conflict(DuplicateAnchor.toJson)
            }
          case AnchorValues(_, Some(weighed)) =>
            AnchorDao.updateWeighed(anchor, weighed) match {
              case 1 => Ok
              case _ => NotFound(AnchorNotFound.toJson)
            }
          case _ => BadRequest // unreachable
        }
      }) match {
        case Right(r) => r
        case Left(r)  => r
      }
    }
  }}

  def delete(prefecture: Int, place: Int, credits: Int, uuid: String, anchored: Long, created_at: Option[Long]) = MaintenanceApiAction { Action { req =>
    DB localTx { implicit session =>
      (for {
        c       <- created_at
        admiral <- AdmiralDao.find(uuid, c)
      } yield {
        AnchorDao.drop(prefecture, place, credits, anchored, admiral) match {
          case 1 => Ok
          case 0 => NotFound(AnchorNotFound.toJson)
        }
      }) match {
        case Some(r) => r
        case _ => BadRequest
      }
    }
  }}

}
