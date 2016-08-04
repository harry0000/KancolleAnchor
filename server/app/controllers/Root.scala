package controllers

import javax.inject._

import arcade.{MaintenanceController, MaintenancePage}
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.twirl.api.Html
import time.ClockProvider

@Singleton
class Root @Inject() (val messagesApi: MessagesApi,
                      val configuration: Configuration,
                      implicit val clockProvider: ClockProvider) extends Controller with I18nSupport with MaintenanceController with MaintenancePage {

  def maintenancePage: Html = views.html.maintenance()

  def index = MaintenancePageAction { Action { implicit request =>
    Ok(views.html.index())
  }}

}
