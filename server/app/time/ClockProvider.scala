package time

import java.time._

trait ClockProvider {
  def clock: Clock
  def now: Instant = Instant.now(clock)
}

object SystemClockProvider extends ClockProvider {
  def clock: Clock = Clock.systemUTC()
}

object TestClockProvider extends ClockProvider {
  var clock: Clock = Clock.systemUTC()

  def fixed(zonedDateTime: ZonedDateTime) = {
    clock = Clock.fixed(zonedDateTime.toInstant, zonedDateTime.getOffset)
  }

  def fixed(offsetTime: OffsetTime) = {
    val offset = offsetTime.getOffset
    clock = Clock.fixed(offsetTime.atDate(LocalDate.now(offset)).toInstant, offset)
  }

  def plusMillis(millisToAdd: Long): Unit = {
    clock = Clock.fixed(clock.instant().plusMillis(millisToAdd), clock.getZone)
  }

  def reset(): Unit = { clock = Clock.systemUTC() }
}
