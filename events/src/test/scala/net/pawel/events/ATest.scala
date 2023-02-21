package net.pawel.events

import net.pawel.events.TicketTailorTimeParsing
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ATest extends AnyFlatSpec with Matchers {

  "Bla" should "bla" in {
    val time = TicketTailorTimeParsing.parseDateAndTime("Sun 2 Oct 12:00 PM")
    println(time)
  }

}