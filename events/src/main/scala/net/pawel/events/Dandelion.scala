package net.pawel.events

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Description
import net.pawel.events.util.Utils.{await, parallelize}
import net.pawel.events.domain.{Event, Organizer, OrganizerType}
import net.pawel.events.util.{RealTime, Time}
import org.jsoup.nodes.{Document, Element}

import java.io.StringReader
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalQueries
import java.time.{LocalDate, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.Locale
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.javaapi.CollectionConverters.asScala
import scala.language.postfixOps
import scala.util.Try

class Dandelion(fetchPage: FetchPage = new FetchPageWithUnirest,
                time: Time = new RealTime) {

  def dateTimeRangeFrom(string: String): (ZonedDateTime, ZonedDateTime) = {
    val regexp = """(.+) – (.*\d+(am|pm)) (.+) \(UTC (.+)\)""".r
    val (start, end, offset) = string match {
      case regexp(start, end, _, _, offset) => (start, end, offset)
    }
    val startTime = parseDateAndTime(start, offset)
    val endTime = parseDateAndTimeOrTime(end, offset, startTime.toLocalDate, startTime.getZone)

    (startTime, endTime)
  }

  def removeOrdinalIndicatorFromDay(string: String): String = {
    val regexp = """(.+\d+)(st|nd|rd|th)(.+)""" r

    string match {
      case regexp(first, _, third) => first + third
    }
  }

  def capitalizeAmPm(string: String) = {
    val length = string.length
    val amPm = string.substring(length - 2)
    string.substring(0, length - 2) + amPm.toUpperCase()
  }

  def parseDateAndTime(string: String, offset: String): ZonedDateTime = {
    val dateAndTimeFormatter = DateTimeFormatter.ofPattern("EEE d LLL uuuu',' h[':'mm]a z", Locale.ENGLISH)
    val cleanedUp = cleanupDateString(string) + " " + offset
    dateAndTimeFormatter.parse(cleanedUp, ZonedDateTime.from(_))
  }

  def parseDateAndTimeOrTime(string: String, offset: String, localDate: LocalDate, zoneId: ZoneId): ZonedDateTime =
    Try(parseDateAndTime(string, offset))
      .getOrElse(parseTime(string, localDate, zoneId))

  private def parseTime(string: String, localDate: LocalDate, zoneId: ZoneId) = {
    val timeFormatter = DateTimeFormatter.ofPattern("h[':'mm]a", Locale.ENGLISH)

    val accessor = timeFormatter.parse(capitalizeAmPm(string))
    val localTime = accessor.query(TemporalQueries.localTime())
    ZonedDateTime.of(LocalDateTime.of(localDate, localTime), zoneId)
  }

  private def cleanupDateString(string: String) =
    capitalizeAmPm(removeOrdinalIndicatorFromDay(string))

  private def toOrganizer(organizerUrl: String): Organizer = {
    val document = fetchPage(organizerUrl)
    val name = document.selectFirst("div.text-center h1").text()
    Organizer(organizerUrl, name, OrganizerType.Dandelion)
  }

  lazy val organizers =
    events.map(_.organizerUrl).distinct.map(toOrganizer)

  lazy val events =
    parallelize(await(eventUrls()), 20)
      .flatMap(tryToFetchEvent)
      .toList

  private def tryToFetchEvent(url: String): Option[Event] = {
    try {
      Some(eventFor(url))
    } catch {
      case exception =>
        println("Dandelion event fetch failed for " + url)
        exception.printStackTrace()
        None
    }
  }

  lazy val organizersToEventsMap = events.groupBy(_.organizerUrl)

  private def eventUrls(): Future[List[String]] = Future {
    val todaysDate = time.today().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val ical = fetchPage.fetchUrl(s"https://dandelion.earth/events.ics?display=&order=&from=$todaysDate&q=&event_tag_id=&search=1")
    System.setProperty("ical4j.unfolding.relaxed", "true")
    val stringReader = new StringReader(ical)
    val builder = new CalendarBuilder
    val calendar = builder.build(stringReader)
    val calendarEvents = calendar.getComponents[VEvent]()
    val events = asScala(calendarEvents).drop(1)
    events
      .map(_.getProperty[Description](Property.DESCRIPTION).get().getValue)
      .toList
  }

  def eventFor(url: String): Event = {
    val document = fetchPage(url)
    val name = document.selectFirst("h1.mb-1").ownText()
    val (startDateTime, endDateTime) = eventDateTimesFrom(document)
    val addressElement = document.select("ul.fa-ul li").get(1)
    val address = addressFrom(addressElement)
    val organizerEventsHref = document.selectFirst(".table-hr td a").attr("href")
    val organizerUrl = "https://dandelion.earth" + organizerEventsHref.replace("/events", "")
    domain.Event(name, url, startDateTime.toInstant, endDateTime.toInstant, address, organizerUrl)
  }

  private def addressFrom(addressElement: Element): String =
    if (addressElement.text() == "Online")
      "Online"
    else {
      addressElement.selectFirst("a").text()
    }

  private def eventDateTimesFrom(document: Document): (ZonedDateTime, ZonedDateTime) = {
    val select = document.selectFirst("select[name='event_id']")
    val selectExists = select != null
    val dateRangeString = if (selectExists) {
      select.selectFirst("option").text()
    } else {
      document.selectFirst("ul.fa-ul li").ownText()
    }
    dateTimeRangeFrom(dateRangeString)
  }
}
