package net.pawel.events

import kong.unirest.Unirest
import net.pawel.events.domain.{Event, Organizer, OrganizerType}
import play.api.libs.json.{JsArray, JsValue, Json}

import java.time.format.DateTimeFormatter
import java.time.temporal.{TemporalAccessor, TemporalQueries}
import java.time.{LocalDate, LocalTime, ZoneId, ZonedDateTime}
import java.util.Locale
import scala.collection.parallel.immutable.ParSeq
import scala.jdk.CollectionConverters._

object EventBrite extends FetchPage {
  private val eventBriteUrl = """https://(www\.eventbrite\.co\.uk|www\.eventbrite\.com)/.+""".r

  private def isEventBriteUrl(url: String): Boolean = eventBriteUrl.matches(url) && !url.contains("/cc/")

  def extractOrganizerUrl(url: String): Option[String] = {
    val page = fetchPage(url)
    val hasPassword = page.selectFirst("#event_password") != null
    val isUnavailable = Option(page.selectFirst(".text-heading-epic")).filter(_.text().contains("This event is currently unavailable."))
      .orElse(Option(page.selectFirst(".text-heading-primary")).filter(_.text().contains("Whoops, the page or event you are looking for was not found.")))
      .isDefined

    if (isUnavailable || hasPassword) {
      None
    } else {
      val anchors = page.select(".expired-organizer__link").asScala ++
        page.select("#organizer-link-org-panel").asScala ++
        page.select(".organizer-info__name a").asScala
      val anchorOption = anchors.headOption
      val result = anchorOption.map(_.attr("href"))
      if (result.isEmpty) {
        println(url)
      }
      result
    }

  }


  def toOrganizerUrl(url: String): Option[String] = {
    if (url.contains("/o/"))
      Some(url)
    else {
      extractOrganizerUrl(url)
    }
  }

  def possibleMultipleDates(url: String): Option[List[(ZonedDateTime, ZonedDateTime)]] = {
    val id = url.substring(url.lastIndexOf("-") + 1)
    val seriesUrl = s"https://www.eventbrite.co.uk/api/v3/series/$id/events/?time_filter=current_future&expand=series_dates%2Cticket_availability%2Cevent_sales_status%2Cvenue&page_size=1000&continuation="
    val response = Unirest.get(seriesUrl).asString()
    if (response.isSuccess) {
      val json = Json.parse(response.getBody)
      Some((json \ "events").as[JsArray].value.map(json => {
        val startTime = localDateTimeFrom(dateTimeFormat.parse((json \ "start" \ "utc").as[String]))
        val endTime = localDateTimeFrom(dateTimeFormat.parse((json \ "end" \ "utc").as[String]))
        (startTime, endTime)
      }).toList)
    } else {
      None
    }
  }

  def toEvent(organizerUrl: String)(json: JsValue): List[Event] = {
    val url = (json \ "url").as[String]
    val dates = possibleMultipleDates(url).getOrElse({
      val startDate = localDateTimeFrom(dateTimeFormat.parse((json \ "startDate").as[String]))
      val endDate = localDateTimeFrom(dateTimeFormat.parse((json \ "endDate").as[String]))
      List((startDate, endDate))
    })
    val name = (json \ "name").as[String]
    val addressType = (json \ "location" \ "@type").as[String]
    val fullAddress = if (addressType == "VirtualLocation") {
      "Online"
    } else {
      val address = json \ "location" \ "address"
      val streetAddress = (address \ "streetAddress").as[String]
      val city = (address \ "addressLocality").as[String]
      val postCode = (address \ "postalCode").as[String]
      s"$streetAddress $city $postCode"
    }

    dates.map {
      case (startDate, endDate) => domain.Event(name, url, startDate.toInstant, endDate.toInstant, fullAddress, organizerUrl)
    }
  }

  val dateTimeFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssX", Locale.ENGLISH)

  def localDateTimeFrom(accessor: TemporalAccessor) = {
    val time = accessor.query(TemporalQueries.localTime())
    val date = accessor.query(TemporalQueries.localDate())
    val timeZone = accessor.query(TemporalQueries.offset())
    ZonedDateTime.of(date, time, timeZone)
  }

  def organizersEvents(organizerPageUrl: String): List[Event] = {
    val page = fetchPage(organizerPageUrl)

    val json = page
      .select("script").asScala
      .filter(_.attr("type") == "application/ld+json")
      .map(_.data().trim)
      .filter(_.startsWith("["))
      .head

    val midnight = ZonedDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT, ZoneId.of("GMT"))
    Json.parse(json)
      .as[JsArray]
      .value
      .flatMap(toEvent(organizerPageUrl))
      .filter(_.start.isAfter(midnight.toInstant))
      .toList
  }

  def fetchOrganizers(allUrls: ParSeq[String]): ParSeq[Organizer] = {
    fetchOrganizerUrls(allUrls)
      .flatMap(url => {
        val page = fetchPage(url)
        val nameOption = page.select("meta").asScala
          .filter(_.attr("property") == "og:title")
          .map(_.attr("content"))
          .headOption
        if (nameOption.isEmpty) {
          println(url)
        }
        nameOption.map(name => Organizer(url = url, name = name, organizerType = OrganizerType.EventBrite))
      })
  }

  def fetchCurrentEvents(allUrls: ParSeq[String]) = {
    val organizerUrls = fetchOrganizerUrls(allUrls)

    println(organizerUrls.mkString("\n"))

    val events = organizerUrls
      .flatMap(organizersEvents)
      .toList

    events
  }

  private def fetchOrganizerUrls(allUrls: ParSeq[String]): ParSeq[String] = {
    val eventBriteUrls = allUrls
      .filter(isEventBriteUrl)
      .distinct

    eventBriteUrls
      .flatMap(toOrganizerUrl)
      .distinct
  }
}
