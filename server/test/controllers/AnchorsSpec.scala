package controllers

import arcade.Errors._
import models.dao._
import models.dto._
import org.scalatestplus.play._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test._
import scalikejdbc._
import time.UTCDateTime
import utils.DBFixture

import scala.concurrent.Future

class AnchorsSpec extends PlaySpec with OneAppPerTest with ArcadeTestSupport with DBFixture with settings.DBSettings {

  import models.JsonConverter._

  def rollback(implicit session: DBSession): Unit = {
    applyUpdate(delete from AdmiralTable)
    applyUpdate(delete from AnchorTable)
    applyUpdate(delete from SpottingTable)
  }

  "list action" should {
    "can get anchors" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor1 = getTestAnchor(admiral)
      val anchor2 = anchor1.copy(number = anchor1.number + 1)
      val created1 = createAnchor(admiral, anchor1)
      val created2 = createAnchor(admiral, anchor2)

      val result = get(anchor1)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Json.toJson(Seq(
        Anchor(
          created2.prefecture,
          created2.place,
          created2.credits,
          created2.page,
          created2.number,
          Some(admiral.id),
          Some(created2.admiral),
          created2.anchored.get,
          None,
          None
        ),
        Anchor(
          created1.prefecture,
          created1.place,
          created1.credits,
          created1.page,
          created1.number,
          Some(admiral.id),
          Some(created1.admiral),
          created1.anchored.get,
          None,
          None
        )
      ))
    }}

    "can get anchors with spotting" in before { withAutoRollback { implicit session =>
      val admiral  = createAdmiral()
      val anchor1  = getTestAnchor(admiral)
      val anchor2  = anchor1.copy(number = anchor1.number + 1)
      val created1 = createAnchor(admiral, anchor1)
      val created2 = createAnchor(admiral, anchor2)

      val spotting1 = createSpotting(createAdmiral(), created1)
      val spotting2 = createSpotting(createAdmiral(), created1)

      spotting1.reported.get must be < spotting2.reported.get

      val result = get(anchor1)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Json.toJson(Seq(
        Anchor(
          created2.prefecture,
          created2.place,
          created2.credits,
          created2.page,
          created2.number,
          Some(admiral.id),
          Some(created2.admiral),
          created2.anchored.get,
          None,
          None
        ),
        Anchor(
          created1.prefecture,
          created1.place,
          created1.credits,
          created1.page,
          created1.number,
          Some(admiral.id),
          Some(created1.admiral),
          created1.anchored.get,
          None,
          Some(SpottingInfo(
            2L,
            spotting1.reported.get
          ))
        )
      ))
    }}

    "return ServiceUnavailable when maintenance" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor  = createAnchor(admiral, getTestAnchor(admiral))

      startMaintenance()

      val result = get(anchor)
      status(result) mustBe SERVICE_UNAVAILABLE
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Maintenance.toJson
    }}
  }

  "create action" should {
    "can create anchor" in  before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor = getTestAnchor(admiral)

      val result = post(admiral, anchor)
      status(result) mustBe CREATED

      val json = contentAsJson(result).validate[Anchor.ForRest].get
      headers(result).get(LOCATION) mustBe Some(s"/anchors/13/0/0/${admiral.admiralId}/${json.anchored.get}")
      json mustBe anchor.copy(anchored = json.anchored)

      findAnchor(admiral, json) mustBe Some(
        Anchor(
          anchor.prefecture,
          anchor.place,
          anchor.credits,
          anchor.page,
          anchor.number,
          Some(admiral.id),
          Some(Admiral(admiral.admiralId, admiral.name)),
          json.anchored.get,
          None,
          None
        )
      )
    }}

    "return ServiceUnavailable when maintenance" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor = getTestAnchor(admiral)

      startMaintenance()

      val result = post(admiral, anchor)
      status(result) mustBe SERVICE_UNAVAILABLE
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Maintenance.toJson
    }}
  }

  "update action" should {
    "can update page and number" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor = createAnchor(admiral, getTestAnchor(admiral))

      val page = anchor.page + 1
      val num  = anchor.number + 1
      val result = put(admiral, anchor, Json.obj("page" -> page, "number" -> num))
      status(result) mustBe OK

      findAnchor(admiral, anchor) mustBe Some(
        Anchor(
          anchor.prefecture,
          anchor.place,
          anchor.credits,
          page,
          num,
          Some(admiral.id),
          Some(Admiral(admiral.admiralId, admiral.name)),
          anchor.anchored.get,
          None,
          None
        )
      )
    }}

    "can update weighed" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor = createAnchor(admiral, getTestAnchor(admiral))

      val weighed = UTCDateTime()
      val result = put(admiral, anchor, Json.obj("weighed" -> weighed.toEpochMilli))
      status(result) mustBe OK

      findAnchor(admiral, anchor) mustBe Some(
        Anchor(
          anchor.prefecture,
          anchor.place,
          anchor.credits,
          anchor.page,
          anchor.number,
          Some(admiral.id),
          Some(Admiral(admiral.admiralId, admiral.name)),
          anchor.anchored.get,
          Some(weighed.toEpochMilli),
          None
        )
      )
    }}

    "return NotFound when update anchor which does not exist" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor  = createAnchor(admiral, getTestAnchor(admiral))

      val weighed = UTCDateTime()
      val result = put(admiral, anchor.copy(anchored = Some(UTCDateTime().toEpochMilli)), Json.obj("weighed" -> weighed.toEpochMilli))
      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe AnchorNotFound.toJson
    }}

    "return ServiceUnavailable when maintenance" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val anchor = createAnchor(admiral, getTestAnchor(admiral))

      startMaintenance()

      val weighed = UTCDateTime()
      val result = put(admiral, anchor, Json.obj("weighed" -> weighed.toEpochMilli))
      status(result) mustBe SERVICE_UNAVAILABLE
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Maintenance.toJson
    }}
  }

  def uri(anchor: Anchor.ForRest): String = s"/anchors/${anchor.prefecture}/${anchor.place}/${anchor.credits}"

  def uri(admiral: Admiral.ForDB): String = s"/anchors?created_at=${admiral.created}"

  def uri(admiral: Admiral.ForDB, anchor: Anchor.ForRest): String = s"/anchors${anchor.location}?created_at=${admiral.created}"

  def getTestAnchor(admiral: Admiral.ForDB): Anchor.ForRest = {
    Anchor(
      prefecture = 13,
      place      =  0,
      credits    =  0,
      page       =  1,
      number     =  1,
      admiral    = Admiral(admiral.admiralId, admiral.name),
      anchored   = None,
      weighed    = None
    )
  }

  def get(anchor: Anchor.ForRest): Future[Result] = {
    route(app, FakeRequest(GET, uri(anchor))).get
  }

  def post(admiral: Admiral.ForDB, anchor: Anchor.ForRest): Future[Result] = {
    route(app, FakeRequest(POST, uri(admiral)).withJsonBody(Json.toJson(anchor))).get
  }

  def put(admiral: Admiral.ForDB, anchor: Anchor.ForRest, json: JsValue): Future[Result] = {
    route(app, FakeRequest(PUT, uri(admiral, anchor)).withJsonBody(json)).get
  }

}
