package models

import inject.UIDInjector
import models.dto._
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

object Constants {
  val maxIdLength   = 255
  val maxNameLength = 32

  val minPrefecture = 1
  val maxPrefecture = 47
}

object JsonConverter extends UIDInjector {
  import Constants._

  private val validAdmiralId:   Reads[String] = minLength[String](1) andKeep maxLength[String](maxIdLength) andKeep verifying(uid.isValid)
  private val validAdmiralName: Reads[String] = minLength[String](1) andKeep maxLength[String](maxNameLength)
  private val validPrefecture:  Reads[Int]    = min(minPrefecture) andKeep max(maxPrefecture)

  implicit val admiralForDBWrites: Writes[Admiral.ForDB] = new Writes[Admiral.ForDB] {
    override def writes(o: Admiral.ForDB): JsValue = Json.obj(
      "admiralId" -> o.admiralId,
      "name"      -> o.name,
      "created"   -> o.created
    )
  }

  implicit val admiralForRestFormat: Format[Admiral.ForRest] = Format((
      (JsPath \ 'admiralId).read[String](validAdmiralId) and
      (JsPath \ 'name     ).readNullable[String](validAdmiralName) and
      (JsPath \ 'created  ).read[Long]
    ).apply((admiralId, name, created) => Admiral(admiralId, name, created)),
    Json.writes[Admiral.ForRest]
  )

  implicit val admiralForJoinFormat: Format[Admiral.ForJoin] = Format((
      (JsPath \ 'admiralId).read[String](validAdmiralId) and
      (JsPath \ 'name     ).readNullable[String](validAdmiralName)
    ).apply((admiralId, name) => Admiral(admiralId, name)),
    Json.writes[Admiral.ForJoin]
  )

  implicit val spottingInfoWrites: Writes[SpottingInfo] = Json.writes[SpottingInfo]

  implicit val anchorForDBWrites: Writes[Anchor.ForDB] = new Writes[Anchor.ForDB] {
    override def writes(o: Anchor.ForDB): JsValue = Json.obj(
      "prefecture" -> o.prefecture,
      "place"      -> o.place,
      "credits"    -> o.credits,
      "page"       -> o.page,
      "number"     -> o.number,
      "admiral"    -> Json.toJson(o.admiral),
      "anchored"   -> o.anchored,
      "weighed"    -> o.weighed,
      "spotting"   -> Json.toJson(o.spotting)
    )
  }

  implicit val anchorForRestFormat: Format[Anchor.ForRest] = Format((
      (JsPath \ 'prefecture).read[Int](validPrefecture) and
      (JsPath \ 'place     ).read[Int](min(0)) and
      (JsPath \ 'credits   ).read[Int](min(0)) and
      (JsPath \ 'page      ).read[Int](min(1)) and
      (JsPath \ 'number    ).read[Int](min(1)) and
      (JsPath \ 'admiral   ).read[Admiral.ForJoin] and
      (JsPath \ 'anchored  ).readNullable[Long] and
      (JsPath \ 'weighed   ).readNullable[Long]
    ).apply { (prefecture, place, credits, page, number, admiral, anchored, weighed) =>
      Anchor(prefecture, place, credits, page, number, admiral, anchored, weighed)
    },
    Json.writes[Anchor.ForRest]
  )

  implicit val spottingReportsForDBWrites: Writes[Spotting.ForDB] = new Writes[Spotting.ForDB] {
    override def writes(o: Spotting.ForDB): JsValue = Json.obj(
      "prefecture" -> o.prefecture,
      "place"      -> o.place,
      "credits"    -> o.credits,
      "page"       -> o.page,
      "number"     -> o.number,
      "spotter"    -> Json.toJson(o.spotter),
      "reported"   -> o.reported
    )
  }

  implicit val spottingReportsForRestFormat: Format[Spotting.ForRest] = Format((
      (JsPath \ 'prefecture).read[Int](validPrefecture) and
      (JsPath \ 'place     ).read[Int](min(0)) and
      (JsPath \ 'credits   ).read[Int](min(0)) and
      (JsPath \ 'page      ).read[Int](min(1)) and
      (JsPath \ 'number    ).read[Int](min(1)) and
      (JsPath \ 'spotter   ).read[Admiral.ForJoin] and
      (JsPath \ 'reported  ).readNullable[Long]
    ).apply { (prefecture, place, credits, page, number, spotter, reported) =>
      Spotting(prefecture, place, credits, page, number, spotter, reported)
    },
    Json.writes[Spotting.ForRest]
  )

}
