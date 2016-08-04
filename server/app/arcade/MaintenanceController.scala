package arcade

import play.api.Configuration
import play.api.mvc._
import play.twirl.api.Html
import time.{ClockProvider, JaTime}

import scala.concurrent.Future

trait MaintenanceController {
  this: Controller =>

  implicit val clockProvider: ClockProvider

  def configuration: Configuration

  private lazy val config = Config(configuration)
  private lazy val start  = config.maintenanceStart
  private lazy val end    = config.maintenanceEnd

  def isMaintenance: Boolean = {
    if (start == end) {
      false
    } else {
      val now = JaTime().toMillis
      start <= now && now < end
    }
  }

  sealed trait MaintenanceAction[A] {
    this: Action[A] =>

    def action: Action[A]
    def result(request: Request[A]): Result

    def apply(request: Request[A]): Future[Result] = {
      if (isMaintenance) {
        Future.successful(result(request))
      } else {
        action(request)
      }
    }

    lazy val parser = action.parser
  }

}

trait MaintenancePage {
  this: Controller with MaintenanceController =>

  def maintenancePage: Html

  case class MaintenancePageAction[A](action: Action[A]) extends Action[A] with MaintenanceAction[A] {
    def result(request: Request[A]): Result = Ok(maintenancePage)
  }
}

trait MaintenanceApi {
  this: Controller with MaintenanceController =>

  import arcade.Errors.Maintenance

  def maintenanceResponse: Result = ServiceUnavailable(Maintenance.toJson)

  case class MaintenanceApiAction[A](action: Action[A]) extends Action[A] with MaintenanceAction[A] {
    def result(request: Request[A]): Result = maintenanceResponse
  }
}
