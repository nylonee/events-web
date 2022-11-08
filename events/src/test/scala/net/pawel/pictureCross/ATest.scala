package net.pawel.pictureCross

import net.pawel.events.ExtractUrls
import net.pawel.events.TicketTailor.TimeParsing
import net.pawel.events.TicketTailor.TimeParsing.dateAndTimeFormatter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalQueries
import java.util.Locale

class ATest extends AnyFlatSpec with Matchers {

  "Bla" should "bla" in {
    val time = TimeParsing.parseDateAndTime("Sun 2 Oct 12:00 PM")
    println(time)
  }

}