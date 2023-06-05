package net.pawel.events

import net.pawel.events.domain.Event
import net.pawel.events.util.ParsingTime.parseInstant
import net.pawel.events.util.{FetchAndWriteToFile, FetchFromFileOrFetchAndWrite, FetchPageFromFile, FixedTime}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{LocalDate, LocalTime, ZoneOffset}

class DandelionTest extends AnyFlatSpec with Matchers {

  "Dandelion.dateTimeRangeFrom" should "work for full date and times" in {
    val (actualStartTime, actualEndTime) = (new Dandelion).dateTimeRangeFrom("Thu 22nd Sep 2022, 7pm – Tue 3rd Jan 2023, 10pm UK time (UTC +01:00)")

    actualStartTime.toLocalDate shouldBe LocalDate.of(2022, 9, 22)
    actualStartTime.toLocalTime shouldBe LocalTime.of(19, 0)
    actualStartTime.getOffset shouldBe ZoneOffset.ofHours(1)

    actualEndTime.toLocalDate shouldBe LocalDate.of(2023, 1, 3)
    actualEndTime.toLocalTime shouldBe LocalTime.of(22, 0)
    actualEndTime.getOffset shouldBe ZoneOffset.ofHours(1)
  }

  "Dandelion.dateTimeRangeFrom" should "work for endtime not containing a date" in {
    val (actualStartTime, actualEndTime) = (new Dandelion).dateTimeRangeFrom("Sun 19th Feb 2023, 3pm – 6:15pm UK time (UTC +00:00)")

    actualStartTime.toLocalDate shouldBe LocalDate.of(2023, 2, 19)
    actualStartTime.toLocalTime shouldBe LocalTime.of(15, 0)
    actualStartTime.getOffset shouldBe ZoneOffset.ofHours(0)

    actualEndTime.toLocalDate shouldBe LocalDate.of(2023, 2, 19)
    actualEndTime.toLocalTime shouldBe LocalTime.of(18, 15)
    actualEndTime.getOffset shouldBe ZoneOffset.ofHours(0)
  }

  "Dandelion.events" should "fetch an event with multiple dates" in {
    val time = FixedTime(LocalDate.of(2023, 2, 28))
    val dandelion = new Dandelion(new FetchPageFromFile, time)
    val event = dandelion.eventFor("https://dandelion.earth/events/63ced42d3d389e000b5f7383")
    event shouldBe Event(
      name = "Full Moon: Kundalini Yoga, Sacred Cacao and Harmonising Sound Journey",
      url = "https://dandelion.earth/events/63ced42d3d389e000b5f7383",
      start = parseInstant("2023-05-06 16:00 Europe/London"),
      end = parseInstant("2023-05-06 19:00 Europe/London"),
      address = "231 Stoke Newington Church St, London N16 9HP, United Kingdom",
      organizerUrl = "https://dandelion.earth/o/the-psychedelic-society")
  }

  "Dandelion.events" should "fetch an event with a single date" in {
    val time = FixedTime(LocalDate.of(2023, 2, 28))
    val dandelion = new Dandelion(new FetchPageFromFile, time)
    val event = dandelion.eventFor("https://dandelion.earth/events/63bbbbc49d91b8000b820665")
    event shouldBe Event(
      name = "Aquarius New Moon Circle",
      url = "https://dandelion.earth/events/63bbbbc49d91b8000b820665",
      start = parseInstant("2023-01-22 19:30 Europe/London"),
      end = parseInstant("2023-01-22 21:00 Europe/London"),
      address = "Online",
      organizerUrl = "https://dandelion.earth/o/cosmoplanner")
  }

//  "Dandelion.events" should "fetch a multidate event / course" in {
//    val time = FixedTime(LocalDate.of(2023, 2, 28))
//    val dandelion = new Dandelion(new FetchPageFromFile, time)
//    val event = dandelion.eventFor("https://dandelion.earth/events/63b57fe3a9c9fd000bfac69e")
//    event shouldBe Event(
//      name = "Headless Way: Online Course with Richard Lang",
//      url = "https://dandelion.earth/events/63b57fe3a9c9fd000bfac69e",
//      start = parseInstant("2023-03-12 19:30 Europe/London"),
//      end = parseInstant("2023-01-22 21:00 Europe/London"),
//      address = "Online",
//      organizerUrl = "https://dandelion.earth/o/cosmoplanner")
//  }

}