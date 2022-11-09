package net.pawel.events

import net.pawel.events.ExtractUrls.extractUrls

import java.time.LocalDate
import scala.io.Source
import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel.ForkJoinTaskSupport

object Events {
  lazy val uri = classOf[TicketTailor.type].getResource("/wa-group-chat.txt").toURI
  lazy val text = Source.fromFile(uri).getLines().mkString("\n")
  lazy val allUrls = extractUrls(text).toList
  lazy val ticketTailorEvents = TicketTailor.fetchEventsOf(TicketTailor.organizerNames)
  lazy val eventBriteEvents = EventBrite.fetchCurrentEvents(allUrls)
  lazy val facebookEvents = Facebook.fetchCurrentEvents(allUrls)
  def allEvents = List(
    () => ticketTailorEvents,
//    () => eventBriteEvents
  ).par.flatMap(_()).toList
    .filterNot(event => event.end.minusDays(3).isAfter(event.start))
    .sortBy(_.start)

  def main(args: Array[String]): Unit = {
    println(TicketTailor.fetchOrganizerNames(allUrls).map(name => s""""$name"""").mkString("List(\n", ",\n", ")"))
//    println(allEvents.filter(_.start.toLocalDate == LocalDate.now()).mkString("\n"))
//    facebookEvents
  }
}

object Facebook extends FetchPage {
  private val eventBriteUrl = """https://(fb\.me|www\.facebook\.com)/.+""".r

  private def isFacebookUrl(url: String): Boolean = eventBriteUrl.matches(url)

  def fetchCurrentEvents(allUrls: List[String]) = {
    val parallel = allUrls.par

    val forkJoinPool = new java.util.concurrent.ForkJoinPool(1000)
    parallel.tasksupport = new ForkJoinTaskSupport(forkJoinPool)

    val facebookUrls = parallel
      .filter(isFacebookUrl)
      .filter(isEventUrl)
      .distinct
      .map(fetchPage)

    println(facebookUrls.mkString("\n"))
  }

  private def isEventUrl(url: String): Boolean =
    url.contains("/e/") || url.contains("/events/")
}

object RankUrls {
  lazy val urlRegex = """https?://(.+?)(/.*)?$""".r

  def main(args: Array[String]): Unit = {
    println(Events.allUrls.map(url => {
      urlRegex.findFirstMatchIn(url).get.group(1)
    }).groupBy(identity).toList.map {
      case (url, list) => (url, list.size)
    }.sortBy(_._2).reverse.mkString("\n"))
  }
}

