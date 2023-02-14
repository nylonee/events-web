package net.pawel.pictureCross

import net.pawel.events.Dandelion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{LocalDate, LocalTime, ZoneOffset}

class DandelionTest extends AnyFlatSpec with Matchers {

  "Dandelion.dateTimeRangeFrom" should "work for full date and times" in {
    val (actualStartTime, actualEndTime) = Dandelion.dateTimeRangeFrom("Thu 22nd Sep 2022, 7pm – Tue 3rd Jan 2023, 10pm UK time (UTC +01:00)")

    actualStartTime.toLocalDate shouldBe LocalDate.of(2022, 9, 22)
    actualStartTime.toLocalTime shouldBe LocalTime.of(19, 0)
    actualStartTime.getOffset shouldBe ZoneOffset.ofHours(1)

    actualEndTime.toLocalDate shouldBe LocalDate.of(2023, 1, 3)
    actualEndTime.toLocalTime shouldBe LocalTime.of(22, 0)
    actualEndTime.getOffset shouldBe ZoneOffset.ofHours(1)
  }

  "Dandelion.dateTimeRangeFrom" should "work for endtime not containing a date" in {
    val (actualStartTime, actualEndTime) = Dandelion.dateTimeRangeFrom("Sun 19th Feb 2023, 3pm – 6:15pm UK time (UTC +00:00)")

    actualStartTime.toLocalDate shouldBe LocalDate.of(2023, 2, 19)
    actualStartTime.toLocalTime shouldBe LocalTime.of(15, 0)
    actualStartTime.getOffset shouldBe ZoneOffset.ofHours(0)

    actualEndTime.toLocalDate shouldBe LocalDate.of(2023, 2, 19)
    actualEndTime.toLocalTime shouldBe LocalTime.of(18, 15)
    actualEndTime.getOffset shouldBe ZoneOffset.ofHours(0)
  }
}