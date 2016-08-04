package models.dto

import models.{Resource, DBModel, RestModel}

sealed case class SpottingForDB(
  prefecture: Int,
  place: Int,
  credits: Int,
  page: Int,
  number: Int,
  spotterId: Option[Long],
  spotter: Option[Admiral.ForJoin],
  reported: Long
)

sealed case class SpottingForRest(
  prefecture: Int,
  place: Int,
  credits: Int,
  page: Int,
  number: Int,
  spotter: Admiral.ForJoin,
  reported: Option[Long]
) extends Resource {
  def location: String = s"/$prefecture/$place/$credits/$page/$number/${spotter.admiralId}"
}

object Spotting extends DBModel[SpottingForDB] with RestModel[SpottingForRest] {

  def apply(
    prefecture: Int,
    place: Int,
    credits: Int,
    page: Int,
    number: Int,
    spotterId: Option[Long],
    spotter: Option[Admiral.ForJoin],
    reported: Long
  ): ForDB = SpottingForDB(prefecture, place, credits, page, number, spotterId, spotter, reported)

  def apply(
    prefecture: Int,
    place: Int,
    credits: Int,
    page: Int,
    number: Int,
    spotter: Admiral.ForJoin,
    reported: Option[Long]
  ): ForRest = SpottingForRest(prefecture, place, credits, page, number, spotter, reported)

}
