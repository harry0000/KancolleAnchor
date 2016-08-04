package arcade

import utils.{EnumCompanion, IntEnumLike}

sealed abstract class DamageLevel(val value: Int) extends IntEnumLike

object DamageLevel extends EnumCompanion[DamageLevel] {
  case object None     extends DamageLevel(0)
  case object Minor    extends DamageLevel(1)
  case object Moderate extends DamageLevel(2)
  case object Heavy    extends DamageLevel(3)
  case object Sank     extends DamageLevel(4)

  lazy val values = Seq(Minor, Moderate, Heavy, Sank)
}
