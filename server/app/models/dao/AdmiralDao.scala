package models.dao

import inject.UIDInjector
import models.dto.Admiral
import scalikejdbc._
import time.UTCDateTime

object AdmiralDao extends UIDInjector {
  private val column = AdmiralTable.column
  private val a      = AdmiralTable.syntax("a")

  def find(admiral: Admiral.ForRest)(implicit session: DBSession): Option[Admiral.ForDB] =
    find(admiral.admiralId, admiral.created)

  def find(admiral: Admiral.ForJoin, created: Long)(implicit session: DBSession): Option[Admiral.ForDB] =
    find(admiral.admiralId, created)

  def find(admiralId: uid.Value, created: Long)(implicit session: DBSession): Option[Admiral.ForDB] = {
    withSQL {
      select
        .from(AdmiralTable as a)
        .where
        .eq(a.admiralId, admiralId).and
        .eq(a.created, UTCDateTime(created).toZoned)
    }.map(rs => AdmiralTable(a)(rs))
      .single()
      .apply()
  }

  def create(created: UTCDateTime)(implicit session: DBSession): Admiral.ForRest = {
    val admiralId = uid.generate

    withSQL {
      insert
        .into(AdmiralTable)
        .namedValues(
          column.admiralId -> admiralId,
          column.created   -> created.toZoned
        )
    }.update
      .apply()

    Admiral(
      admiralId,
      None,
      created.toEpochMilli
    )
  }

  def updateName(admiral: Admiral.ForDB, name: Option[String])(implicit session: DBSession): Int = {
    withSQL {
      update(AdmiralTable)
        .set(column.name -> name)
        .where.eq(column.id, admiral.id)
    }.update
      .apply()
  }

  def drop(admiral: Admiral.ForDB)(implicit session: DBSession): Int = {
    withSQL {
      delete
        .from(AdmiralTable)
        .where.eq(column.id, admiral.id)
    }.update
      .apply()
  }
}

sealed case class AdmiralTable(
  id: Long,
  admiralId: String,
  name: Option[String] = None,
  created: java.sql.Timestamp
)

object AdmiralTable extends SQLSyntaxSupport[AdmiralTable] {
  override val tableName = "admiral"
  override val columns = Seq("id", "admiral_id", "name", "created")

  def apply(a: SyntaxProvider[AdmiralTable])(rs: WrappedResultSet): Admiral.ForDB = apply(a.resultName)(rs)
  def apply(a: ResultName[AdmiralTable])(rs: WrappedResultSet): Admiral.ForDB =
    Admiral(
      rs.get(a.id),
      rs.get(a.admiralId),
      rs.get(a.name),
      UTCDateTime(rs.timestamp(a.created)).toEpochMilli
    )

  def join(a: SyntaxProvider[AdmiralTable])(rs: WrappedResultSet): Option[Admiral.ForJoin] = join(a.resultName)(rs)
  def join(a: ResultName[AdmiralTable])(rs: WrappedResultSet): Option[Admiral.ForJoin] =
    rs.longOpt(a.id).map { _ =>
      Admiral(
        rs.get(a.admiralId),
        rs.get(a.name)
      )
    }

}
