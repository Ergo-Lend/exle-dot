package io.persistence.doobs.models

import java.sql.{Connection, PreparedStatement, ResultSet}
import java.time.LocalDateTime

object Models {
  abstract class DatabaseConversion[T] {
    protected def fromResultSet(resultSet: ResultSet): T

    protected def str(idx: Int)(implicit rs: ResultSet): String = {
      rs.getString(idx)
    }

    protected def long(idx: Int)(implicit rs: ResultSet): Long = {
      rs.getLong(idx)
    }

    protected def date(idx: Int)(implicit rs: ResultSet): LocalDateTime = {
      rs.getObject(idx, classOf[LocalDateTime])
    }

    protected def double(idx:Int)(implicit rs: ResultSet): Double = {
      rs.getDouble(idx)
    }

    protected def int(idx: Int)(implicit rs: ResultSet): Int = {
      rs.getInt(idx)
    }

    protected def bool(idx: Int)(implicit rs: ResultSet): Boolean = {
      rs.getBoolean(idx)
    }
  }

  case class DbConn(conn: Connection) {
    def state(s: String): PreparedStatement = conn.prepareStatement(s)

    def close(): Unit = conn.close()
  }
}
