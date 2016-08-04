package models

trait Resource {
  def location: String
}

trait DBModel[T] {
  type ForDB = T
}

trait RestModel[T <: Resource] {
  type ForRest = T
}

trait JoinModel[T] {
  type ForJoin = T
}
