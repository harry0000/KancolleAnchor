package arcade

import java.time.OffsetTime
import java.time.temporal.ChronoField.MILLI_OF_DAY

import play.api.Configuration

case class Config(conf: Configuration) {

  def maintenanceStart: Long = conf.getString("anchor.maintenance.start").map(OffsetTime.parse(_).getLong(MILLI_OF_DAY)).getOrElse(0L)
  def maintenanceEnd:   Long = conf.getString("anchor.maintenance.end").map(OffsetTime.parse(_).getLong(MILLI_OF_DAY)).getOrElse(0L)

  def leastSpottingInterval: Long = conf.getLong("anchor.spotting.leastInterval").getOrElse(0L)

}
