package models.dao

import models.dto.{Admiral, Anchor, Spotting, SpottingInfo}
import scalikejdbc._
import time.UTCDateTime

object AnchorDao {
  private val column = AnchorTable.column
  private val anc    = AnchorTable.syntax("anc")
  private val a      = AdmiralTable.syntax("a")
  private val s      = SpottingTable.syntax("s")

  def find(prefecture: Int, place: Int, credits: Int, anchored: Long, admiral: Admiral.ForDB)(implicit session: DBSession): Option[Anchor.ForDB] = {
    val x = SubQuery.syntax("x").include(s)

    withSQL {
      select(anc.result.*, a.result.*, sqls"x.hits", sqls"x.first_reported")
        .from(AnchorTable as anc)
        .innerJoin(AdmiralTable as a).on(a.id, anc.admiralId)
        .leftJoin {
          select(
            s.result.prefecture,
            s.result.place,
            s.result.credits,
            s.result.page,
            s.result.number,
            sqls"${sqls.count} as hits",
            sqls"${sqls.min(s.reported)} as first_reported"
          )
            .from(SpottingTable as s)
            .where
            .eq(s.prefecture, prefecture).and
            .eq(s.place,      place).and
            .eq(s.credits,    credits)
            .groupBy(s.prefecture, s.place, s.credits, s.page, s.number) as x
        }.on(sqls
          .eq(x(s).prefecture, anc.prefecture).and
          .eq(x(s).place,      anc.place).and
          .eq(x(s).credits,    anc.credits).and
          .eq(x(s).page,       anc.page).and
          .eq(x(s).number,     anc.number)
        )
        .where
        .eq(anc.prefecture, prefecture).and
        .eq(anc.place,      place).and
        .eq(anc.credits,    credits).and
        .eq(anc.admiralId,  admiral.id).and
        .eq(anc.anchored,   UTCDateTime(anchored).toZoned)
    }.map(rs => AnchorTable(anc, a)(rs))
      .single
      .apply()
  }

  def list(prefecture: Int, place: Int, credits: Int)(implicit session: DBSession): Seq[Anchor.ForDB] = {
    val x = SubQuery.syntax("x").include(s)

    withSQL {
      select(anc.result.*, a.result.*, sqls"x.hits", sqls"x.first_reported")
        .from(AnchorTable as anc)
        .leftJoin(AdmiralTable as a).on(anc.admiralId, a.id)
        .leftJoin {
          select(
            s.result.prefecture,
            s.result.place,
            s.result.credits,
            s.result.page,
            s.result.number,
            sqls"${sqls.count} as hits",
            sqls"${sqls.min(s.reported)} as first_reported"
          )
            .from(SpottingTable as s)
            .where
            .eq(s.prefecture, prefecture).and
            .eq(s.place,      place).and
            .eq(s.credits,    credits)
            .groupBy(s.prefecture, s.place, s.credits, s.page, s.number) as x
        }.on(sqls
          .eq(x(s).prefecture, anc.prefecture).and
          .eq(x(s).place,      anc.place).and
          .eq(x(s).credits,    anc.credits).and
          .eq(x(s).page,       anc.page).and
          .eq(x(s).number,     anc.number)
        )
        .where
        .eq(anc.prefecture, prefecture).and
        .eq(anc.place,      place).and
        .eq(anc.credits,    credits)
        .orderBy(anc.page.desc, anc.number.desc, anc.anchored.desc)
    }.map(rs => AnchorTable(anc, a)(rs))
      .list
      .apply()
  }

  def summarySpotting(spotting: Spotting.ForRest, admiral: Admiral.ForDB)(implicit session: DBSession): Option[AnchorSpotting] = {
    import spotting.{prefecture, place, credits, page, number}
    val last_reported = SubQuery.syntax("last_reported").include(s)
    val x = SubQuery.syntax("x").include(s)

    withSQL {
      select(
        sqls"spotting_count",
        select(sqls.max(s.reported))
          .from(SpottingTable as s)
          .where.eq(s.spotterId, admiral.id) as last_reported
      )
        .from(AnchorTable as anc)
        .leftJoin {
          select(
            s.result.*,
            sqls"${sqls.count} as spotting_count"
          )
            .from(SpottingTable as s)
            .where
            .eq(s.prefecture, prefecture).and
            .eq(s.place,      place).and
            .eq(s.credits,    credits).and
            .eq(s.page,       page).and
            .eq(s.number,     number)
            .groupBy(s.prefecture, s.place, s.credits, s.page, s.number) as x
        }.on(sqls
          .eq(x(s).prefecture, anc.prefecture).and
          .eq(x(s).place,      anc.place).and
          .eq(x(s).credits,    anc.credits).and
          .eq(x(s).page,       anc.page).and
          .eq(x(s).number,     anc.number)
        )
        .where
        .notExists {
          select
            .from(AnchorTable as anc)
            .where
            .eq(anc.prefecture, prefecture).and
            .eq(anc.place,      place).and
            .eq(anc.credits,    credits).and
            .eq(anc.page,       page).and
            .eq(anc.number,     number).and
            .isNotNull(anc.weighed)
        }.and
        .eq(anc.prefecture, prefecture).and
        .eq(anc.place,      place).and
        .eq(anc.credits,    credits).and
        .eq(anc.page,       page).and
        .eq(anc.number,     number)
        .groupBy(anc.prefecture, anc.place, anc.credits, anc.page, anc.number)
    }.map { rs =>
      AnchorSpotting(
        rs.longOpt(1).getOrElse(0L),
        rs.timestampOpt(2).map(UTCDateTime(_))
      )
    }.single
      .apply()
  }

  def create(anchor: Anchor.ForRest, admiral: Admiral.ForDB, anchored: UTCDateTime)(implicit session: DBSession): Anchor.ForRest = {
    import anchor.{prefecture, place, credits, page, number}

    withSQL {
      insert
        .into(AnchorTable)
        .namedValues(
          column.prefecture -> prefecture,
          column.place      -> place,
          column.credits    -> credits,
          column.page       -> page,
          column.number     -> number,
          column.admiralId  -> admiral.id,
          column.anchored   -> anchored.toZoned
        )
    }.update
      .apply()

    anchor.copy(anchored = Some(anchored.toEpochMilli))
  }

  def updatePosition(anchor: Anchor.ForDB, page: Int, number: Int)(implicit session: DBSession): Int = {
    withSQL {
      update(AnchorTable)
        .set(
          column.page   -> page,
          column.number -> number
        )
        .where
        .eq(column.prefecture, anchor.prefecture).and
        .eq(column.place,      anchor.place).and
        .eq(column.credits,    anchor.credits).and
        .eq(column.admiralId,  anchor.admiralId).and
        .eq(column.anchored,   UTCDateTime(anchor.anchored).toZoned)
    }.update
      .apply()
  }

  def updateWeighed(anchor: Anchor.ForDB, weighed: Long)(implicit session: DBSession): Int = {
    withSQL {
      update(AnchorTable)
        .set(
          column.weighed -> UTCDateTime(weighed).toZoned
        )
        .where
        .eq(column.prefecture, anchor.prefecture).and
        .eq(column.place,      anchor.place).and
        .eq(column.credits,    anchor.credits).and
        .eq(column.admiralId,  anchor.admiralId).and
        .eq(column.anchored,   UTCDateTime(anchor.anchored).toZoned)
    }.update
      .apply()
  }

  def drop(prefecture: Int, place: Int, credits: Int, anchored: Long, admiral: Admiral.ForDB)(implicit session: DBSession): Int = {
    withSQL {
      deleteFrom(AnchorTable)
        .where
        .eq(column.prefecture, prefecture).and
        .eq(column.place,      place).and
        .eq(column.credits,    credits).and
        .eq(column.admiralId,  admiral.id).and
        .eq(column.anchored,   UTCDateTime(anchored).toZoned)
    }.update
      .apply()
  }

}

case class AnchorSpotting(
  reportedCount: Long,
  lastReported: Option[UTCDateTime]
)

sealed case class AnchorTable(
  prefecture: Int,
  place: Int,
  credits: Int,
  page: Int,
  number: Int,
  admiralId: Option[Long] = None,
  anchored: java.sql.Timestamp,
  weighed: Option[java.sql.Timestamp]
)

object AnchorTable extends SQLSyntaxSupport[AnchorTable] {

  override val tableName = "anchor"
  override val columns = Seq("prefecture", "place", "credits", "page", "number", "admiral_id", "anchored", "weighed")

  def apply(anc: SyntaxProvider[AnchorTable], a: SyntaxProvider[AdmiralTable])(rs: WrappedResultSet): Anchor.ForDB = apply(anc.resultName, a.resultName)(rs)
  def apply(anc: ResultName[AnchorTable], a: ResultName[AdmiralTable])(rs: WrappedResultSet): Anchor.ForDB =
    Anchor(
      rs.get(anc.prefecture),
      rs.get(anc.place),
      rs.get(anc.credits),
      rs.get(anc.page),
      rs.get(anc.number),
      rs.longOpt(anc.admiralId),
      AdmiralTable.join(a)(rs),
      UTCDateTime(rs.timestamp(anc.anchored)).toEpochMilli,
      rs.timestampOpt(anc.weighed).map(UTCDateTime(_).toEpochMilli),
      rs.longOpt("hits").map { hits =>
        SpottingInfo(hits, UTCDateTime(rs.timestamp("first_reported")).toEpochMilli)
      }
    )

}
