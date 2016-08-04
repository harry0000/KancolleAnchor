package time

import java.time._
import java.time.temporal.ChronoField

sealed trait Offset {
  def offset: ZoneOffset
}

sealed trait UTC extends Offset {
  def offset = ZoneOffset.UTC
}

sealed trait Ja extends Offset {
  def offset = ZoneOffset.ofHours(9)
}

sealed trait DateTime {
  def toZoned: ZonedDateTime

  private lazy val instant = toZoned.toInstant
  def toEpochMilli: Long = instant.toEpochMilli
}

case class UTCDateTime private (toZoned: ZonedDateTime) extends DateTime
object UTCDateTime extends UTC {
  def apply()(implicit clockProvider: ClockProvider): UTCDateTime = apply(clockProvider.now)
  def apply(timestamp: java.sql.Timestamp): UTCDateTime = apply(timestamp.getTime)
  def apply(epochMilli: Long): UTCDateTime = apply(Instant.ofEpochMilli(epochMilli))
  def apply(instant: Instant): UTCDateTime = UTCDateTime(instant.atZone(offset))
}

sealed trait Time {
  val toOffsetTime: OffsetTime
  def toMillis: Long = toOffsetTime.getLong(ChronoField.MILLI_OF_DAY)
}

case class UTCTime private (toOffsetTime: OffsetTime) extends Time
object UTCTime extends UTC {
  def apply()(implicit clockProvider: ClockProvider): UTCTime = apply(clockProvider.now)
  def apply(epochMilli: Long): UTCTime = apply(Instant.ofEpochMilli(epochMilli))
  def apply(instant: Instant): UTCTime = UTCTime(OffsetTime.ofInstant(instant, offset))
}

case class JaTime private (toOffsetTime: OffsetTime) extends Time
object JaTime extends Ja {
  def apply()(implicit clockProvider: ClockProvider): JaTime = apply(clockProvider.now)
  def apply(epochMilli: Long): JaTime = apply(Instant.ofEpochMilli(epochMilli))
  def apply(instant: Instant): JaTime = JaTime(OffsetTime.ofInstant(instant, offset))
}

case class Duration private (asJava: java.time.Duration) {
  def toMillis: Long = asJava.toMillis
}
object Duration {
  def apply(start: UTCDateTime, end: UTCDateTime): Duration =
    Duration(java.time.Duration.between(start.toZoned, end.toZoned))
}
