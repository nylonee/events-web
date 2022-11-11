package net.pawel.events

import net.pawel.events.ExtractUrls.extractUrls

import scala.collection.parallel.CollectionConverters._
import scala.io.Source

object Events {
  lazy val uri = classOf[TicketTailor.type].getResource("/wa-group-chat.txt").toURI
  lazy val text = Source.fromFile(uri).getLines().mkString("\n")
  lazy val allUrls = extractUrls(text).toList
  lazy val ticketTailorOrganizers = TicketTailor.fetchOrganizers(allUrls)
  lazy val ticketTailorEvents = ticketTailorOrganizers.flatMap(organizer => TicketTailor.fetchOrganizerEvents(organizer.url))
  lazy val eventBriteEvents = EventBrite.fetchCurrentEvents(allUrls)
  lazy val eventBriteOrganizers = EventBrite.fetchOrganizers(allUrls)
  lazy val facebookEvents = Facebook.fetchCurrentEvents(allUrls)
  lazy val allOrganizers = ticketTailorOrganizers ++ eventBriteOrganizers

  def allEvents = List(
    () => ticketTailorEvents,
    () => eventBriteEvents
  ).par.flatMap(_()).toList
    .filterNot(event => event.end.minusDays(3).isAfter(event.start))
    .sortBy(_.start)

  def main(args: Array[String]): Unit = {
//    println(ticketTailorEvents.mkString("\n"))
    println(eventBriteOrganizers.mkString("\n"))
//    facebookEvents
  }
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



