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

  "organizersEvents" should "fetch organizer's events with multiple dates" in {
    val eventBrite = new EventBrite(new FetchPageFromFile)
    val events = eventBrite.organizersEvents("https://www.eventbrite.co.uk/o/togetherness-8057382611")
    events shouldBe List(
      Event("Boundaries Lab: Intro to the Wheel of Consent",
        "https://www.eventbrite.co.uk/e/boundaries-lab-intro-to-the-wheel-of-consent-tickets-368828003487",
        parseInstant("2023-02-23 19:00 Europe/London"),
        parseInstant("2023-02-23 21:00 Europe/London"),
        "Octavius St London SE8 4BY",
        "https://www.eventbrite.co.uk/o/togetherness-8057382611"
      ),
      Event(
        "Boundaries Lab: Intro to the Wheel of Consent",
        "https://www.eventbrite.co.uk/e/boundaries-lab-intro-to-the-wheel-of-consent-tickets-368828003487",
        parseInstant("2023-09-07 19:00 Europe/London"),
        parseInstant("2023-09-07 21:00 Europe/London"),
        "Octavius St London SE8 4BY",
        "https://www.eventbrite.co.uk/o/togetherness-8057382611"
      ),
      Event("Silent Walk & Circling",
        "https://www.eventbrite.co.uk/e/silent-walk-circling-tickets-528136048137",
        parseInstant("2023-03-05 11:00 Europe/London"),
        parseInstant("2023-03-05 18:00 Europe/London"),
        "Blackheath Ave London SE10 8XJ",
        "https://www.eventbrite.co.uk/o/togetherness-8057382611"
      ),
      Event(
        "Practice Club: Wheel of Consent",
        "https://www.eventbrite.co.uk/e/practice-club-wheel-of-consent-tickets-551618274107",
        parseInstant("2023-03-16 19:00 Europe/London"),
        parseInstant("2023-03-16 21:30 Europe/London"),
        "Sky Dome London SE8 4BY",
        "https://www.eventbrite.co.uk/o/togetherness-8057382611"
      ),
      Event(
        "Practice Club: Wheel of Consent",
        "https://www.eventbrite.co.uk/e/practice-club-wheel-of-consent-tickets-551618274107",
        parseInstant("2023-04-13 19:00 Europe/London"),
        parseInstant("2023-04-13 21:30 Europe/London"),
        "Sky Dome London SE8 4BY",
        "https://www.eventbrite.co.uk/o/togetherness-8057382611"
      ),
      Event(
        "Practice Club: Wheel of Consent",
        "https://www.eventbrite.co.uk/e/practice-club-wheel-of-consent-tickets-551618274107",
        parseInstant("2023-05-18 19:00 Europe/London"),
        parseInstant("2023-05-18 21:30 Europe/London"),
        "Sky Dome London SE8 4BY",
        "https://www.eventbrite.co.uk/o/togetherness-8057382611"
      ),
      Event(
        "Practice Club: Wheel of Consent",
        "https://www.eventbrite.co.uk/e/practice-club-wheel-of-consent-tickets-551618274107",
        parseInstant("2023-06-15 19:00 Europe/London"),
        parseInstant("2023-06-15 21:30 Europe/London"),
        "Sky Dome London SE8 4BY",
        "https://www.eventbrite.co.uk/o/togetherness-8057382611"
      )
    )
  }
}