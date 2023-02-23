package net.pawel.events

import net.pawel.events.domain.Event
import net.pawel.events.util.Time.parseInstant
import net.pawel.events.util.{FetchAndWriteToFile, FetchPageFromFile}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EventBriteTest extends AnyFlatSpec with Matchers {

  "organizersEvents" should "fetch organizer's events" in {
    val eventBrite = new EventBrite(new FetchPageFromFile)
    val events = eventBrite.organizersEvents("https://www.eventbrite.co.uk/o/ukdc-15634844127")
    events shouldBe List(
      Event(
        "Free Brazilian Zouk Latin Dance Class - Thurs 16th March",
        "https://www.eventbrite.co.uk/e/free-brazilian-zouk-latin-dance-class-thurs-16th-march-tickets-519706926407",
        parseInstant("2023-03-16 19:15 Europe/London"),
        parseInstant("2023-03-16 20:30 Europe/London"),
        "24 Haverstock Hill London NW3 2BQ",
        "https://www.eventbrite.co.uk/o/ukdc-15634844127"
      ),
      Event("Free Brazilian Zouk Latin Dance Class - Thurs 20th April",
        "https://www.eventbrite.co.uk/e/free-brazilian-zouk-latin-dance-class-thurs-20th-april-tickets-549309879637",
        parseInstant("2023-04-20 19:15 Europe/London"),
        parseInstant("2023-04-20 20:30 Europe/London"),
        "New Kent Rd London SE1 4AN",
        "https://www.eventbrite.co.uk/o/ukdc-15634844127"
      ))
  }

}