package controllers

import arcade.Errors._
import models.dao._
import models.dto._
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test._
import scalikejdbc._
import time.{TestClockProvider, UTCDateTime}
import utils.DBFixture

import scala.concurrent.Future

class SpottingsSpec extends PlaySpec with OneAppPerTest with ArcadeTestSupport with DBFixture with settings.DBSettings {

  import models.JsonConverter._

  def rollback(implicit session: DBSession): Unit = {
    applyUpdate(delete from AdmiralTable)
    applyUpdate(delete from AnchorTable)
    applyUpdate(delete from SpottingTable)
  }

  "find action" should {
    "can find spotting" in  before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor = createAnchor(admiral, Anchor(13, 0, 0, 1, 1, Admiral(admiral.admiralId, admiral.name), None, None))
      val spotting = createSpotting(admiral, anchor)

      val result = get(admiral, spotting)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      contentAsJson(result) mustBe Json.toJson(Seq(
        Spotting(
          spotting.prefecture,
          spotting.place,
          spotting.credits,
          spotting.page,
          spotting.number,
          Some(admiral.id),
          Some(spotting.spotter),
          spotting.reported.get
        )
      ))
    }}

    "return ServiceUnavailable when maintenance" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor = createAnchor(admiral, Anchor(13, 0, 0, 1, 1, Admiral(admiral.admiralId, admiral.name), None, None))
      val spotting = createSpotting(admiral, anchor)

      startMaintenance()

      val result = get(admiral, spotting)
      status(result) mustBe SERVICE_UNAVAILABLE
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Maintenance.toJson
    }}
  }

  "create action" should {
    "can create spotting" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor = createAnchor(admiral, Anchor(13, 0, 0, 1, 1, Admiral(admiral.admiralId, admiral.name), None, None))
      val spotter = createAdmiral()
      val spotting = toSpotting(spotter, anchor)

      val result = post(spotter, spotting)
      status(result) mustBe CREATED
      contentType(result) mustBe Some("application/json")

      val json = contentAsJson(result).validate[Spotting.ForRest].get
      headers(result).get(LOCATION) mustBe Some(s"/spottings/13/0/0/1/1/${spotter.admiralId}")
      json mustBe spotting.copy(reported = json.reported)

      findSpotting(spotter, json) mustBe Seq(
        Spotting(
          json.prefecture,
          json.place,
          json.credits,
          json.page,
          json.number,
          Some(spotter.id),
          Some(Admiral(
            spotter.admiralId,
            spotter.name
          )),
          json.reported.get
        )
      )

      summarySpotting(spotter, json) mustBe json.reported.map(reported => AnchorSpotting(1, Some(UTCDateTime(reported))))
    }}

    "can create when post with a least interval" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor1 = createAnchor(admiral, Anchor(13, 0, 0, 1, 1, Admiral(admiral.admiralId, admiral.name), None, None))
      val anchor2 = createAnchor(admiral, Anchor(13, 0, 0, 1, 2, Admiral(admiral.admiralId, admiral.name), None, None))
      val spotter = createAdmiral()

      val post1 = post(spotter, anchor1)
      status(post1) mustBe CREATED
      contentType(post1) mustBe Some("application/json")

      TestClockProvider.plusMillis(app.configuration.getLong("anchor.spotting.leastInterval").get)

      val post2 = post(spotter, anchor2)
      status(post2) mustBe CREATED
      contentType(post2) mustBe Some("application/json")
    }}

    "forbidden when spotting posted in quick succession" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor1 = createAnchor(admiral, Anchor(13, 0, 0, 1, 1, Admiral(admiral.admiralId, admiral.name), None, None))
      val anchor2 = createAnchor(admiral, Anchor(13, 0, 0, 1, 2, Admiral(admiral.admiralId, admiral.name), None, None))
      val spotter = createAdmiral()

      val created = post(spotter, anchor1)
      status(created) mustBe CREATED
      contentType(created) mustBe Some("application/json")

      val forbidden = post(spotter, anchor2)
      status(forbidden) mustBe FORBIDDEN
      contentType(forbidden) mustBe Some("application/json")
      contentAsJson(forbidden) mustBe TooShortSpottingInterval.toJson
    }}

    "conflict when spotting to same anchor" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor  = createAnchor(admiral, Anchor(13, 0, 0, 1, 1, Admiral(admiral.admiralId, admiral.name), None, None))
      val spotter = createAdmiral()

      val created = post(spotter, anchor)
      status(created) mustBe CREATED
      contentType(created) mustBe Some("application/json")

      TestClockProvider.plusMillis(app.configuration.getLong("anchor.spotting.leastInterval").get)

      val conflict = post(spotter, anchor)
      status(conflict) mustBe CONFLICT
      contentType(conflict) mustBe Some("application/json")
      contentAsJson(conflict) mustBe AnchorAlreadySpotted.toJson
    }}

    "return BadRequest when spotting posted to anchor which does not exist " in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val spotter = createAdmiral()

      val result = post(spotter, Spotting(13, 0, 0, 1, 1, Admiral(admiral.admiralId, None), None))
      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe AnchorNotFound.toJson
    }}

    "return ServiceUnavailable when maintenance" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor = createAnchor(admiral, Anchor(13, 0, 0, 1, 1, Admiral(admiral.admiralId, admiral.name), None, None))
      val spotter = createAdmiral()
      val spotting = toSpotting(spotter, anchor)

      startMaintenance()

      val result = post(spotter, spotting)
      status(result) mustBe SERVICE_UNAVAILABLE
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Maintenance.toJson
    }}
  }

  def uri(admiral: Admiral.ForDB): String =
    s"/spottings?created_at=${admiral.created}"

  def uri(admiral: Admiral.ForDB, spotting: Spotting.ForRest): String =
    s"/spottings/${spotting.prefecture}/${spotting.place}?uid=${spotting.spotter.admiralId}&created_at=${admiral.created}"

  def get(admiral: Admiral.ForDB, spotting: Spotting.ForRest): Future[Result] = {
    route(app, FakeRequest(GET, uri(admiral, spotting))).get
  }

  def post(spotter: Admiral.ForDB, spotting: Spotting.ForRest): Future[Result] = {
    route(app, FakeRequest(POST, uri(spotter)).withJsonBody(Json.toJson(spotting))).get
  }

  def post(spotter: Admiral.ForDB, anchor: Anchor.ForRest): Future[Result] = {
    route(app, FakeRequest(POST, uri(spotter)).withJsonBody(Json.toJson(toSpotting(spotter, anchor)))).get
  }

}
