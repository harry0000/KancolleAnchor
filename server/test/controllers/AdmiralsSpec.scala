package controllers

import arcade.Errors.Maintenance
import models.dao._
import models.dto.Admiral
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import scalikejdbc._
import utils.DBFixture

class AdmiralsSpec extends PlaySpec with OneAppPerTest with ArcadeTestSupport with DBFixture with settings.DBSettings {

  import models.JsonConverter._

  def rollback(implicit session: DBSession): Unit = {
    applyUpdate(delete from AdmiralTable)
  }

  "create action" should {
    "can create admiral" in before { withAutoRollback { implicit session =>
      val result = route(app, FakeRequest(POST, "/admirals")).get
      status(result) mustBe CREATED
      contentType(result) mustBe Some("application/json")

      val admiral = contentAsJson(result).validate[Admiral.ForRest].get
      headers(result).get(LOCATION) mustBe Some("/admirals" + admiral.location)

      val created = DB readOnly { implicit session => AdmiralDao.find(admiral).get }
      created.admiralId mustBe admiral.admiralId
      created.created   mustBe admiral.created
      created.name      mustBe None
    }}

    "return ServiceUnavailable when maintenance" in before { withAutoRollback { implicit session =>
      startMaintenance()

      val result = route(app, FakeRequest(POST, "/admirals")).get
      status(result) mustBe SERVICE_UNAVAILABLE
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Maintenance.toJson
    }}
  }

  "update action" should {
    "can update name" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val name = Some("名無し提督")
      val result =
        route(
          app,
          FakeRequest(PUT, uri(admiral))
            .withJsonBody(Json.toJson(Admiral(admiral.admiralId, name, admiral.created)))
        ).get

      status(result) mustBe OK

      val updated = DB readOnly { implicit session => AdmiralDao.find(admiral.admiralId, admiral.created).get }
      updated.admiralId mustBe admiral.admiralId
      updated.created mustBe admiral.created
      updated.name mustBe name
    }}

    "return ServiceUnavailable when maintenance" in before { withAutoRollback { implicit session =>
      val admiral = createAdmiral()
      val name = Some("名無し提督")

      startMaintenance()

      val result =
        route(
          app,
          FakeRequest(PUT, uri(admiral))
            .withJsonBody(Json.toJson(Admiral(admiral.admiralId, name, admiral.created)))
        ).get

      status(result) mustBe SERVICE_UNAVAILABLE
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Maintenance.toJson
    }}
  }

  def uri(admiral: Admiral.ForDB): String = uri(Admiral(admiral.admiralId, admiral.name, admiral.created))

  def uri(admiral: Admiral.ForRest): String = "/admirals" + admiral.location

}
