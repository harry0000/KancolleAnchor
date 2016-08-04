package utils

trait Before {

  def before(): Unit

  def before(test: => Unit) {
    before()
    test
  }

}
