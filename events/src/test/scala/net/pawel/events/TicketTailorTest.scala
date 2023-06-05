package net.pawel.events

import net.pawel.events.domain.{Event, Organizer, OrganizerType}
import net.pawel.events.util.ParsingTime.parseInstant
import net.pawel.events.util.{FetchPageFromFile, ParsingTime}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time._

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

  "fetchOrganizer" should "extract organizer's name from image" in {
    val ticketTailor = new TicketTailor(new FetchPageFromFile)
    val organizer = ticketTailor.fetchOrganizer("https://www.tickettailor.com/events/rapture")
    organizer shouldBe Some(
      Organizer(
        "https://www.tickettailor.com/events/rapture",
        "RAPTURE - High Energy Ecstatic Dance",
        OrganizerType.TicketTailor)
    )
  }

  "fetchOrganizer" should "handle nonexistent / inactive organizer page" in {
    val ticketTailor = new TicketTailor(new FetchPageFromFile)
    val organizer = ticketTailor.fetchOrganizer("https://www.tickettailor.com/events/wilddancer")
    organizer shouldBe None
  }

   "dateRangeFrom" should "extract date range for BST" in {
    val ticketTailor = new TicketTailor(new FetchPageFromFile)
    val (from, to) = ticketTailor.dateRangeFrom("Tue 11 Apr 2023 6:45 PM - 9:30 PM BST")
     val date = LocalDate.of(2023, 4, 11)
     val zoneId = ZoneId.of("Europe/London")
     from shouldBe ZonedDateTime.of(date, LocalTime.of(18, 45), zoneId)
     to shouldBe ZonedDateTime.of(date, LocalTime.of(21, 30), zoneId)
  }

  "dateRangeFrom" should "extract date range for GMT" in {
    val ticketTailor = new TicketTailor(new FetchPageFromFile)
    val (from, to) = ticketTailor.dateRangeFrom("Thu 2 Feb 2023 6:45 PM - 9:30 PM GMT")
     val date = LocalDate.of(2023, 2, 2)
     val zoneId = ZoneId.of("GMT")
     from shouldBe ZonedDateTime.of(date, LocalTime.of(18, 45), zoneId)
     to shouldBe ZonedDateTime.of(date, LocalTime.of(21, 30), zoneId)
  }

  "fetchOrganizerEvents" should "extract correct event information for an event with multiple dates" in {
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

  "fetchOrganizerEvents" should "extract correct event information for one organizer's events" in {
    val ticketTailor = new TicketTailor(new FetchPageFromFile)
    val allEvents = ticketTailor.fetchOrganizerEvents("https://www.tickettailor.com/events/dancelondon")

    def onlyForro(event: Event): Boolean = event.name == "Brazilian Partner Dance Forró Night - every Monday!"

    def onlyMovingConnections(event: Event): Boolean = event.name == "Moving Connections - Playful Encounters through Dance!"

    allEvents.size shouldBe 45

    val forroEvents = allEvents.filter(onlyForro)
    forroEvents.size shouldBe 27
    val movingConnections = allEvents.filter(onlyMovingConnections)
    movingConnections.size shouldBe 14

    movingConnections.foreach(event => {
      event.name shouldBe "Moving Connections - Playful Encounters through Dance!"
      event.url shouldBe "https://www.tickettailor.com/events/dancelondon/583732/"
      event.address shouldBe "Open House Hackney, E9 5LX"
      event.organizerUrl shouldBe "https://www.tickettailor.com/events/dancelondon"
    })

    movingConnections.map(event => (event.start, event.end)) shouldBe List(
      movingConnectionsEventTimesFor(month = 2, day = 28),
      movingConnectionsEventTimesFor(month = 3, day = 7),
      movingConnectionsEventTimesFor(month = 3, day = 14),
      movingConnectionsEventTimesFor(month = 3, day = 21),
      movingConnectionsEventTimesFor(month = 3, day = 28),
      movingConnectionsEventTimesFor(month = 4, day = 4),
      movingConnectionsEventTimesFor(month = 4, day = 11),
      movingConnectionsEventTimesFor(month = 4, day = 18),
      movingConnectionsEventTimesFor(month = 4, day = 25),
      movingConnectionsEventTimesFor(month = 5, day = 2),
      movingConnectionsEventTimesFor(month = 5, day = 9),
      movingConnectionsEventTimesFor(month = 5, day = 16),
      movingConnectionsEventTimesFor(month = 5, day = 23),
      movingConnectionsEventTimesFor(month = 5, day = 30),
    )

    val otherEvents = allEvents.filterNot(onlyForro).filterNot(onlyMovingConnections)
    otherEvents shouldBe List(
      Event("Moving Connections, Contact Improvisation, Dancing, Sharing, Saunas and Camping in Nature!",
        "https://www.tickettailor.com/events/dancelondon/812462/",
        parseInstant("2023-06-09 16:00 Europe/London"),
        parseInstant("2023-06-11 19:00 Europe/London"),
        "Bellingdon and Asheridge Village Hall, HP5 2XU",
        "https://www.tickettailor.com/events/dancelondon"
      ),
      Event(
        "Connecting with our Sensual Selves through Movement, Intimacy, Presence, Saunas and Camping in Nature!",
        "https://www.tickettailor.com/events/dancelondon/819749/",
        parseInstant("2023-06-16 16:00 Europe/London"),
        parseInstant("2023-06-18 19:00 Europe/London"),
        "Bellingdon and Asheridge Village Hall, HP5 2XU",
        "https://www.tickettailor.com/events/dancelondon"
      ),
      Event("Rumble 2023 - A festival devoted to its dancers - Part 1",
        "https://www.tickettailor.com/events/dancelondon/831409/",
        parseInstant("2023-08-03 14:00 Europe/London"),
        parseInstant("2023-08-07 23:59 Europe/London"),
        "Bellingdon and Asheridge Village Hall, HP5 2XU",
        "https://www.tickettailor.com/events/dancelondon"
      ),
      Event("Rumble 2023 - A festival devoted to its dancers - Part 2",
        "https://www.tickettailor.com/events/dancelondon/833184/",
        parseInstant("2023-08-10 14:00 Europe/London"),
        parseInstant("2023-08-14 23:59 Europe/London"),
        "Bellingdon and Asheridge Village Hall, HP5 2XU",
        "https://www.tickettailor.com/events/dancelondon"
      ),
    )
  }

  private def caringPlaceEventTimesFor(month: Int, day: Int) = {
    val zoneId: ZoneId = ZoneId.of("Europe/London")
    val date = LocalDate.of(2023, month, day)
    val startTime = LocalTime.of(18, 0)
    val endTime = LocalTime.of(21, 15)
    (ZonedDateTime.of(date, startTime, zoneId).toInstant,
      ZonedDateTime.of(date, endTime, zoneId).toInstant)
  }

  private def movingConnectionsEventTimesFor(month: Int, day: Int) = {
    val zoneId: ZoneId = ZoneId.of("Europe/London")
    val date = LocalDate.of(2023, month, day)
    val startTime = LocalTime.of(18, 45)
    val endTime = LocalTime.of(21, 30)
    (ZonedDateTime.of(date, startTime, zoneId).toInstant,
      ZonedDateTime.of(date, endTime, zoneId).toInstant)
  }
}