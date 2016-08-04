package utils

trait EnumLike {
  type Value
  def value: Value
}

trait IntEnumLike extends EnumLike {
  type Value = Int
}

trait EnumCompanion[A <: EnumLike] {

  def values: Seq[A]

  def valueOf(value: A#Value): Option[A] = values.find(_.value == value)

}
