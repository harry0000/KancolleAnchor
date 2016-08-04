package arcade

trait UID {
  type Value = String
  def generate: Value
  def isValid(id: Value): Boolean
}

object UUID extends UID {
  private val UUIDRegex = """\p{Alnum}{8}(?:-\p{Alnum}{4}){3}-\p{Alnum}{12}"""

  def generate: String = java.util.UUID.randomUUID().toString

  def isValid(id: String): Boolean = id.matches(UUIDRegex)
}
