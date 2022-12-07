package net.pawel.events

import net.pawel.events.domain.{Event, Organizer, OrganizerType}
import org.jsoup.nodes.{Document, Element}

import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalQueries
import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneId, ZonedDateTime}
import java.util.Locale
import scala.collection.parallel.CollectionConverters._
import scala.jdk.CollectionConverters._
import scala.util.Try

object TicketTailor extends FetchPage {
  private val ticketTailor = """https://(www\.tickettailor\.com/events|buytickets\.at)/([^/]+)(/(\d)+#?/?(\?.+)?)?""".r

  private def isTicketTailorUrl(url: String): Boolean = ticketTailor.matches(url)

  private def extractNameFromUrl(url: String): String = ticketTailor.findFirstMatchIn(url).get.group(2)

  object TimeParsing {
    val dateAndTimeWithTimezoneFormatter = DateTimeFormatter.ofPattern("EEE d LLL uuuu h:mm a z", Locale.ENGLISH)
    val dateAndTimeFormatter = DateTimeFormatter.ofPattern("EEE d LLL uuuu h:mm a", Locale.ENGLISH)
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    def parseTimeOrDate(defaultDate: LocalDate, string: String, timezoneSuffix: String): ZonedDateTime = {
      val accessor = Try {
        timeFormatter.parse(string)
      }
        .orElse(Try {
          dateAndTimeWithTimezoneFormatter.parse((string + " " + timezoneSuffix).trim)
        })
        .orElse(Try {
          dateAndTimeFormatter.parse(string)
        })
        .get
      val time = accessor.query(TemporalQueries.localTime())
      val date = Option(accessor.query(TemporalQueries.localDate()))
        .getOrElse(if (time == LocalTime.MIDNIGHT) defaultDate.plusDays(1) else defaultDate)
      val timeZone = ZoneId.of(if (timezoneSuffix.isEmpty || timezoneSuffix == "BST") "GMT" else timezoneSuffix)
      ZonedDateTime.of(date, time, timeZone)
    }

    def parseDateAndTime(string: String): ZonedDateTime = {
      val accessor =
        Try {
          dateAndTimeWithTimezoneFormatter.parse(string)
        }
          .orElse(Try {
            dateAndTimeFormatter.parse(string)
          })
          .get

      val time = accessor.query(TemporalQueries.localTime())
      val date = accessor.query(TemporalQueries.localDate())
      val timeZone = Option(accessor.query(TemporalQueries.zoneId()))
      ZonedDateTime.of(date, time, timeZone.getOrElse(ZoneId.of("GMT")))
    }
  }

  val moreRegexp = """.+?( +\+.\d+.more.dates?)""".r

  private def stripMore(string: String) = moreRegexp.findFirstMatchIn(string) match {
    case Some(theMatch) => {
      string.substring(0, string.size - theMatch.group(1).size)
    }
    case None => {
      string
    }
  }

  private def dateRangeFrom(string: String): (ZonedDateTime, ZonedDateTime) = {
    val (rangeString, timezoneSuffix) = if (string.matches(""".+ [A-Z]{3}""")) {
      (string.substring(0, string.length - 4), string.substring(string.length - 3))
    } else {
      (string, "")
    }
    val Array(fromString, toString) = rangeString.split(" - ")
    val fromDateTime = TimeParsing.parseDateAndTime((fromString + " " + timezoneSuffix).trim)
    val toDateTime = TimeParsing.parseTimeOrDate(fromDateTime.toLocalDate, toString, timezoneSuffix)

    (fromDateTime, toDateTime)
  }

  def extractMultipleDates(eventUrl: String): List[(ZonedDateTime, ZonedDateTime)] = {
    val page = fetchPage(eventUrl + "select-date")
    page.select(".select_date .date")
      .asScala
      .toList
      .filterNot(_.select(".date_portion").isEmpty)
      .map(element => {
        val dateString = element.selectFirst(".date_portion").wholeText()
        val timeString = element.selectFirst(".time_portion").wholeText()
        dateRangeFrom(dateString + timeString)
      })
  }

  private def extractEvent(organizerUrl: String)(eventDetails: Element): List[Event] = {
    Try {
      val element = eventDetails.selectFirst("a")
      val href = element.attr("href")
      val eventLink = s"https://www.tickettailor.com$href"
      val eventName = eventDetails.selectFirst(".name").text()
      val address = Option(eventDetails.selectFirst(".venue")).map(_.text()).getOrElse("")

      val dateRanges: List[(ZonedDateTime, ZonedDateTime)] = extractEventDates(eventLink)
      dateRanges.map {
        case (fromDate, toDate) => domain.Event(eventName, eventLink, fromDate.toInstant, toDate.toInstant, address, organizerUrl)
      }
    }.recover {
      case e: Throwable =>
        println(
          s"""Event parsing failed:
             |Organizer url: $organizerUrl
             |EventDetails: $eventDetails
             |""".stripMargin)
        e.printStackTrace()
        Nil
    }.get
  }

  private def extractEventDates(eventLink: String): List[(ZonedDateTime, ZonedDateTime)] = {
    val eventPage = fetchPage(eventLink)
    if (eventPage.select(".password_protected").first() != null) {
      Nil
    } else {
      val dateAndTimeElement = eventPage.selectFirst(".date_and_venue h2")
      val dateAndTimeText = dateAndTimeElement.wholeText().trim
      val hasMultipleDates = dateAndTimeText.contains("Multiple dates and times")
      if (hasMultipleDates) {
        extractMultipleDates(eventLink)
      } else {
        val moreStripped = stripMore(dateAndTimeText)
        val (fromDate, toDate) = dateRangeFrom(moreStripped)
        List((fromDate, toDate))
      }
    }
  }

  private def fetchEventsFor(name: String): List[Event] = {
    val organizerPageUrl = organizerEventsPageUrl(name)
    fetchOrganizerEvents(organizerPageUrl)
  }

  def fetchOrganizerEvents(organizerPageUrl: String): List[Event] = {
    val page = fetchPage(organizerPageUrl)

    val listings = page
      .select(".event_listing")
      .asScala.toList
      .filterNot(_.attr("class").contains("no_events"))

    listings.flatMap(extractEvent(organizerPageUrl))
  }

  private def organizerEventsPageUrl(name: String): String = s"https://www.tickettailor.com/events/$name"

  def fetchCurrentEvents(urls: List[String]): List[Event] = {
    val distinctNames = fetchOrganizerNames(urls)

    fetchEventsOf(distinctNames)
  }

  def fetchEventsOf(organizerNames: List[String]) =
    organizerNames.par.flatMap(fetchEventsFor).toList

  def fetchOrganizerNames(urls: List[String]) = {
    urls
      .filter(isTicketTailorUrl)
      .distinct
      .map(extractNameFromUrl)
      .distinct
  }

  def fetchOrganizers(urls: List[String]): List[Organizer] = {
    fetchOrganizerNames(urls)
      .map(organizerEventsPageUrl)
      .flatMap(url => {
        val page = fetchPage(url)

        if (page.wholeText().contains("This page is not available right now.")) {
          None
        } else {
          val header = page.selectFirst("#global_large_header")
          val name =
            Option(header.selectFirst("h1")).map(_.text())
              .orElse(Option(header.selectFirst("img")).map(_.attr("alt")))
              .get
          Some(Organizer(url = url, name = name, organizerType = OrganizerType.TicketTailor))
        }
      })
  }
}
