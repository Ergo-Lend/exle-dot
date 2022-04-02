package ergotools

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
}
