package net.pawel.events.util

import java.time.{Instant, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.Locale

object ParsingTime {
  private val dateAndTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-d HH:mm VV", Locale.ENGLISH)

  def parseZonedTime(dateTime: String): ZonedDateTime =
    dateAndTimeFormatter.parse(dateTime, ZonedDateTime.from(_))

  def parseInstant(dateTime: String): Instant = parseZonedTime(dateTime).toInstant
}
