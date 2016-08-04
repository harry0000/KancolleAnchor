package utils

import scalikejdbc.{DB, DBSession}

trait DBFixture {

  def rollback(implicit session: DBSession): Unit

  def withAutoRollback(test: DBSession => Unit): Unit = {
    DB autoCommit { session =>
      try {
        test(session)
      }
      finally {
        rollback(session)
      }
    }
  }

}
