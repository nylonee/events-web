package net.pawel.events

import org.jsoup.nodes.Document
import play.api.libs.json.{JsArray, JsValue, Json}

import java.time.format.DateTimeFormatter
import java.time.temporal.{TemporalAccessor, TemporalQueries}
import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.Locale
import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel.ForkJoinTaskSupport
import scala.jdk.CollectionConverters._

object EventBrite extends FetchPage {
  private val eventBriteUrl = """https://(www\.eventbrite\.co\.uk|www\.eventbrite\.com)/.+""".r

  private def isEventBriteUrl(url: String): Boolean = eventBriteUrl.matches(url)

  def extractOrganizerUrl(document: Document): Option[String] = {
    val hasPassword = document.selectFirst("#event_password") != null
    val isUnavailable = Option(document.selectFirst(".text-heading-epic")).filter(_.text().contains("This event is currently unavailable.")).isDefined

    if (isUnavailable || hasPassword) {
      None
    } else {
      val anchors = document.select(".expired-organizer__link").asScala ++
        document.select("#organizer-link-org-panel").asScala ++
        document.select(".organizer-info__name a").asScala
      val anchor = anchors.head
      val result = anchor.attr("href")
      Some(result)
    }
  }


  def toOrganizerUrl(url: String): Option[String] = {
    if (url.contains("/o/"))
      Some(url)
    else {
      val page = fetchPage(url)
      extractOrganizerUrl(page)
    }
  }

  def extractEvents(organizerPage: Document): List[Event] = {
    val json = organizerPage
      .select("script").asScala
      .filter(_.attr("type") == "application/ld+json")
      .map(_.data().trim)
      .filter(_.startsWith("["))
      .head

    Json.parse(json)
      .as[JsArray]
      .value
      .map(toEvent)
      .filter(_.start.isAfter(LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)))
      .toList
  }

  def toEvent(json: JsValue): Event = {
    val startDate = localDateTimeFrom(dateTimeFormat.parse((json \ "startDate").as[String]))
    val endDate = localDateTimeFrom(dateTimeFormat.parse((json \ "endDate").as[String]))
    val name = (json \ "name").as[String]
    val url = (json \ "url").as[String]
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

    Event(name, url, startDate, endDate, fullAddress)
  }

  val dateTimeFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)

  def localDateTimeFrom(accessor: TemporalAccessor) = {
    val time = accessor.query(TemporalQueries.localTime())
    val date = accessor.query(TemporalQueries.localDate())
    LocalDateTime.of(date, time)
  }

  def organizersEvents(organizerPageUrl: String): List[Event] = {
    val page = fetchPage(organizerPageUrl)
    extractEvents(page)
  }

  def fetchCurrentEvents(allUrls: List[String]) = {
    val parallel = allUrls.par

    val forkJoinPool = new java.util.concurrent.ForkJoinPool(1000)
    parallel.tasksupport = new ForkJoinTaskSupport(forkJoinPool)

    val eventBriteUrls = parallel
      .filter(isEventBriteUrl)
      .distinct

    val organizerUrls = eventBriteUrls
      .flatMap(toOrganizerUrl)
      .distinct

    println(organizerUrls.mkString("\n"))

    val events = organizerUrls
      .flatMap(organizersEvents)
      .toList

    events
  }
}
