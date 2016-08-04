package controllers

import java.time.OffsetTime

import models.dao._
import models.dto._
import org.scalatest.TestData
import org.scalatestplus.play.OneAppPerTest
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import scalikejdbc._
import time.{ClockProvider, TestClockProvider, UTCDateTime}
import utils.Before

trait ArcadeTestSupport extends Before {
  this: OneAppPerTest =>

  implicit val clockProvider: ClockProvider = TestClockProvider

  implicit override def newAppForTest(td: TestData): Application =
    new GuiceApplicationBuilder()
      .configure(
        "db.default.driver" -> "org.h2.Driver",
        "db.default.url"    -> "jdbc:h2:mem:test;MODE=MYSQL;DB_CLOSE_DELAY=-1",
        "anchor.maintenance.start"      -> "02:00+09:00",
        "anchor.maintenance.end"        -> "06:00+09:00",
        "anchor.spotting.leastInterval" -> 540000
      )
      .overrides(bind[ClockProvider].toInstance(TestClockProvider))
      .build()

  def before(): Unit = {
    finishMaintenance()
  }

  def startMaintenance(): Unit = {
    TestClockProvider.fixed(
      OffsetTime.parse(app.configuration.getString("anchor.maintenance.start").get)
    )
  }

  def finishMaintenance(): Unit = {
    TestClockProvider.fixed(
      OffsetTime.parse(app.configuration.getString("anchor.maintenance.end").get)
    )
  }

  protected def createAdmiral()(implicit session: DBSession): Admiral.ForDB =
    AdmiralDao.find(AdmiralDao.create(UTCDateTime())).get

  protected def createAnchor(admiral: Admiral.ForDB, anchor: Anchor.ForRest)(implicit session: DBSession): Anchor.ForRest = {
    val created = AnchorDao.create(anchor, admiral, UTCDateTime())
    TestClockProvider.plusMillis(1L)
    created
  }

  protected def findAnchor(admiral: Admiral.ForDB, anchor: Anchor.ForRest)(implicit session: DBSession): Option[Anchor.ForDB] =
    anchor.anchored.flatMap { a =>
      import anchor.{prefecture, place, credits}
      AnchorDao.find(prefecture, place, credits, a, admiral)
    }

  protected def findSpotting(admiral: Admiral.ForDB, spotting: Spotting.ForRest)(implicit session: DBSession): Seq[Spotting.ForDB] =
    SpottingDao.list(spotting.prefecture, spotting.place, admiral)

  protected def createSpotting(spotter: Admiral.ForDB, anchor: Anchor.ForRest)(implicit session: DBSession): Spotting.ForRest = {
    val created = SpottingDao.create(toSpotting(spotter, anchor), spotter, UTCDateTime())
    TestClockProvider.plusMillis(1L)
    created
  }

  protected def summarySpotting(admiral: Admiral.ForDB, spotting: Spotting.ForRest)(implicit session: DBSession): Option[AnchorSpotting] =
    AnchorDao.summarySpotting(spotting, admiral)

  protected def toSpotting(spotter: Admiral.ForDB, anchor: Anchor.ForRest): Spotting.ForRest =
    Spotting(
      anchor.prefecture,
      anchor.place,
      anchor.credits,
      anchor.page,
      anchor.number,
      Admiral(
        spotter.admiralId,
        spotter.name
      ),
      None
    )

}
