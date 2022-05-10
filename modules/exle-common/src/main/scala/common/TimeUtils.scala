package common

import java.time.{Duration, LocalDateTime}

object TimeUtils {
  def fromNow(from: LocalDateTime): Duration = {
    val duration = Duration.between(from, LocalDateTime.now())

    duration
  }

  def hasPassed(from: LocalDateTime, minute: Long): Boolean = {
    val duration = fromNow(from)

    if (duration.toMinutes >= minute)
      true
    else
      false
  }

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ns")
    result
  }
}
