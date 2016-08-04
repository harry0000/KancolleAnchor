package models.dto

import models.{Resource, DBModel, RestModel}

case class SpottingInfo(
  hits: Long,
  firstReported: Long
)

sealed case class AnchorForDB(
  prefecture: Int,
  place: Int,
  credits: Int,
  page: Int,
  number: Int,
  admiralId: Option[Long],
  admiral: Option[Admiral.ForJoin],
  anchored: Long,
  weighed: Option[Long],
  spotting: Option[SpottingInfo]
)

sealed case class AnchorForRest(
  prefecture: Int,
  place: Int,
  credits: Int,
  page: Int,
  number: Int,
  admiral: Admiral.ForJoin,
  anchored: Option[Long],
  weighed: Option[Long]
) extends Resource {
  def location: String = s"/$prefecture/$place/$credits/${admiral.admiralId}${anchored.fold("")("/" + _)}"
}

object Anchor extends DBModel[AnchorForDB] with RestModel[AnchorForRest] {

  def apply(
    prefecture: Int,
    place: Int,
    credits: Int,
    page: Int,
    number: Int,
    admiralId: Option[Long],
    admiral: Option[Admiral.ForJoin],
    anchored: Long,
    weighed: Option[Long],
    spotting: Option[SpottingInfo]
  ): ForDB = AnchorForDB.apply(prefecture, place, credits, page, number, admiralId, admiral, anchored, weighed, spotting)

  def apply(
     prefecture: Int,
     place: Int,
     credits: Int,
     page: Int,
     number: Int,
     admiral: Admiral.ForJoin,
     anchored: Option[Long],
     weighed: Option[Long]
  ): ForRest = AnchorForRest.apply(prefecture, place, credits, page, number, admiral, anchored, weighed)

}
