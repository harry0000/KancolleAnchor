package models.dto

import models.{Resource, DBModel, JoinModel, RestModel}

sealed case class AdmiralForDB(
  id: Long,
  admiralId: String,
  name: Option[String],
  created: Long
)

sealed  case class AdmiralForRest(
  admiralId: String,
  name: Option[String],
  created: Long
) extends Resource {
  def location: String = s"/$admiralId"
}

sealed case class AdmiralForJoin(
  admiralId: String,
  name: Option[String]
)

object Admiral extends DBModel[AdmiralForDB] with RestModel[AdmiralForRest] with JoinModel[AdmiralForJoin] {

  def apply(
    id: Long,
    admiralId: String,
    name: Option[String],
    created: Long
  ): ForDB = AdmiralForDB.apply(id, admiralId, name, created)

  def apply(
    admiralId: String,
    name: Option[String],
    created: Long
  ): ForRest = AdmiralForRest.apply(admiralId, name, created)

  def apply(
    admiralId: String,
    name: Option[String]
  ): ForJoin = AdmiralForJoin(admiralId, name)

}
