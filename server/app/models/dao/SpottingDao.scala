package models.dao

import models.dto.{Admiral, Spotting}
import scalikejdbc._
import time.UTCDateTime

object SpottingDao {

  val column = SpottingTable.column
  val s      = SpottingTable.syntax("s")
  val a      = AdmiralTable.syntax("a")
  val anc    = AnchorTable.syntax("anc")

  def list(prefecture: Int, place: Int, admiral: Admiral.ForDB)(implicit session: DBSession): Seq[Spotting.ForDB] = {
    withSQL {
      select
        .from(SpottingTable as s)
        .innerJoin(AdmiralTable as a).on(a.id, s.spotterId)
        .where
        .eq(column.prefecture, prefecture).and
        .eq(column.place,      place).and
        .eq(column.spotterId,  admiral.id)
        .orderBy(column.credits.asc, column.page.desc, column.number.desc)
    }.map(SpottingTable(s, a))
      .list
      .apply()
  }

  def create(report: Spotting.ForRest, spotter: Admiral.ForDB, reported: UTCDateTime)(implicit session: DBSession): Spotting.ForRest = {
    withSQL {
      insert
        .into(SpottingTable)
        .namedValues(
          column.prefecture -> report.prefecture,
          column.place      -> report.place,
          column.credits    -> report.credits,
          column.page       -> report.page,
          column.number     -> report.number,
          column.spotterId  -> spotter.id,
          column.reported   -> reported.toZoned
        )
    }.update
      .apply()

    report.copy(reported = Some(reported.toEpochMilli))
  }

}

sealed case class SpottingTable(
  prefecture: Int,
  place: Int,
  credits: Int,
  page: Int,
  number: Int,
  spotterId: Option[Long],
  reported: java.sql.Timestamp
)

object SpottingTable extends SQLSyntaxSupport[SpottingTable] {

  override val tableName = "weigh_anchor_spotting"
  override val columns = Seq("prefecture", "place", "credits", "page", "number", "spotter_id", "reported")

  def apply(s: SyntaxProvider[SpottingTable], a: SyntaxProvider[AdmiralTable])(rs: WrappedResultSet): Spotting.ForDB = apply(s.resultName, a.resultName)(rs)
  def apply(s: ResultName[SpottingTable], a: ResultName[AdmiralTable])(rs: WrappedResultSet): Spotting.ForDB =
    Spotting(
      rs.get(s.prefecture),
      rs.get(s.place),
      rs.get(s.credits),
      rs.get(s.page),
      rs.get(s.number),
      rs.longOpt(s.spotterId),
      AdmiralTable.join(a)(rs),
      UTCDateTime(rs.timestamp(s.reported)).toEpochMilli
    )

}
