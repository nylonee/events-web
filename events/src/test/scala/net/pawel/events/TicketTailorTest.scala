package net.pawel.events

import net.pawel.events.domain.{Organizer, OrganizerType}
import net.pawel.events.util.FetchPageFromFile
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{LocalDate, LocalTime, ZoneId, ZonedDateTime}

class TicketTailorTest extends AnyFlatSpec with Matchers {

  "fetchOrganizer" should "extract correct events information" in {
    val ticketTailor = new TicketTailor(new FetchPageFromFile)
    val organizer = ticketTailor.fetchOrganizer("https://www.tickettailor.com/events/acaringplace")
    organizer shouldBe Some(
      Organizer(
        "https://www.tickettailor.com/events/acaringplace",
        "A Caring Place",
        OrganizerType.TicketTailor)
    )
  }

  "fetchOrganizerEvents" should "extract correct event information" in {
    val ticketTailor = new TicketTailor(new FetchPageFromFile)
    val events = ticketTailor.fetchOrganizerEvents("https://www.tickettailor.com/events/acaringplace")
    events.foreach(event => {
      event.name shouldBe "A Caring Place"
      event.url shouldBe "https://www.tickettailor.com/events/acaringplace/839749/"
      event.address shouldBe "12 Greenhills Terrace, N1 3QD"
      event.organizerUrl shouldBe "https://www.tickettailor.com/events/acaringplace"
    })

    events.map(event => (event.start, event.end)) shouldBe List(
      caringPlaceEventTimesFor(month = 3, day = 3),
      caringPlaceEventTimesFor(month = 3, day = 10),
      caringPlaceEventTimesFor(month = 3, day = 17),
      caringPlaceEventTimesFor(month = 3, day = 24),
      caringPlaceEventTimesFor(month = 3, day = 31),
      caringPlaceEventTimesFor(month = 4, day = 7),
      caringPlaceEventTimesFor(month = 4, day = 14),
      caringPlaceEventTimesFor(month = 4, day = 21),
      caringPlaceEventTimesFor(month = 4, day = 28),
      caringPlaceEventTimesFor(month = 5, day = 5),
      caringPlaceEventTimesFor(month = 5, day = 12),
      caringPlaceEventTimesFor(month = 5, day = 19),
    )
  }

  private def caringPlaceEventTimesFor(month: Int, day: Int) = {
    val date = LocalDate.of(2023, month, day)
    val startTime = LocalTime.of(18, 0)
    val endTime = LocalTime.of(21, 15)
    (ZonedDateTime.of(date, startTime, ZoneId.of("GMT")).toInstant,
      ZonedDateTime.of(date, endTime, ZoneId.of("GMT")).toInstant)
  }
}