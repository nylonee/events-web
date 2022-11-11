package net.pawel.events

import net.pawel.events.domain.{Event, Organizer, OrganizerType}
import org.jsoup.nodes.{Document, Element}

import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalQueries
import java.time.{LocalDate, LocalDateTime}
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

    def parseTimeOrDate(defaultDate: LocalDate, string: String, timezoneSuffix: String): LocalDateTime = {
      val accessor = Try {
        timeFormatter.parse(string)
      }
        .orElse(Try {
          dateAndTimeWithTimezoneFormatter.parse(string + timezoneSuffix)
        })
        .orElse(Try {
          dateAndTimeFormatter.parse(string)
        })
        .get
      val time = accessor.query(TemporalQueries.localTime())
      val date = Option(accessor.query(TemporalQueries.localDate())).getOrElse(defaultDate)
      LocalDateTime.of(date, time)
    }

    def parseDateAndTime(string: String): LocalDateTime = {
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
      LocalDateTime.of(date, time)
    }
  }

  val moreRegexp = """.+?( +\+.\d+.more.dates)""".r

  private def stripMore(string: String) = moreRegexp.findFirstMatchIn(string) match {
    case Some(theMatch) => {
      string.substring(0, string.size - theMatch.group(1).size)
    }
    case None => {
      string
    }
  }

  private def dateRangeFrom(string: String): (LocalDateTime, LocalDateTime) = {
    val (rangeString, timezoneSuffix) = if (string.matches(""".+ [A-Z]{3}""")) {
      (string.substring(0, string.length - 4), string.substring(string.length - 4))
    } else {
      (string, "")
    }
    val Array(fromString, toString) = rangeString.split(" - ")
    val fromDateTime = TimeParsing.parseDateAndTime(fromString + timezoneSuffix)
    val toDateTime = TimeParsing.parseTimeOrDate(fromDateTime.toLocalDate, toString, timezoneSuffix)

    (fromDateTime, toDateTime)
  }

  def extractMultipleDates(eventUrl: String): List[(LocalDateTime, LocalDateTime)] = {
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

  private def extractEvent(eventDetails: Element): List[Event] = {
    val element = eventDetails.selectFirst("a")
    val href = element.attr("href")
    val eventLink = s"https://www.tickettailor.com$href"
    val eventName = eventDetails.selectFirst(".name").text()
    val address = Option(eventDetails.selectFirst(".venue")).map(_.text()).getOrElse("")

    val dateRanges: List[(LocalDateTime, LocalDateTime)] = extractEventDates(eventLink)
    dateRanges.map {
      case (fromDate, toDate) => domain.Event(eventName, eventLink, fromDate, toDate, address)
    }
  }

  private def extractEventDates(eventLink: String): List[(LocalDateTime, LocalDateTime)] = {
    val eventPage = fetchPage(eventLink)
    if (eventPage.select(".password_protected").first() != null) {
      Nil
    } else {
      val dateAndTimeElement = eventPage.selectFirst(".date_and_venue h2")
      val hasMultipleDates = dateAndTimeElement.wholeText().contains("Multiple dates and times")
      if (hasMultipleDates) {
        extractMultipleDates(eventLink)
      } else {
        val trimmed = dateAndTimeElement.wholeText().trim
        val moreStripped = stripMore(trimmed)
        val (fromDate, toDate) = dateRangeFrom(moreStripped)
        List((fromDate, toDate))
      }
    }
  }

  private def extractEvents(document: Document): List[Event] = {
    val listings = document
      .select(".event_listing")
      .asScala.toList
      .filterNot(_.attr("class").contains("no_events"))

    listings.flatMap(extractEvent)
  }

  private def fetchEventsFor(name: String): List[Event] = {
    val organizerPageUrl = organizerEventsPageUrl(name)
    fetchOrganizerEvents(organizerPageUrl)
  }

  def fetchOrganizerEvents(organizerPageUrl: String): List[Event] = {
    val page = fetchPage(organizerPageUrl)
    extractEvents(page)
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
