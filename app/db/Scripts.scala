package db

import ai.snips.bsonmacros.DatabaseContext
import db.UpdateEvents.eventRepository
import net.pawel.events.domain.{Event, Organizer, OrganizerType}
import net.pawel.events.{EventBrite, Events, TicketTailor}
import org.mongodb.scala.bson.collection.immutable.Document
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Mode}

import java.io.File
import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait Repositories {

  private val configuration = Configuration
    .load(Environment(new File("."), Configuration.getClass.getClassLoader, Mode.Dev))

  private val lifecycle = new ApplicationLifecycle {
    override def addStopHook(hook: () => Future[_]): Unit = {}

    override def stop(): Future[_] = Future.successful({})
  }
  private val context = new DatabaseContext(configuration, lifecycle)
  val organizerRepository = new OrganizerRepository(context)
  val eventRepository = new EventRepository(context)
}

trait Common {
  def eventsOf(organizer: Organizer): List[Event] = {
    organizer.organizerType match {
      case OrganizerType.TicketTailor => TicketTailor.fetchOrganizerEvents(organizer.url)
      case OrganizerType.EventBrite => EventBrite.organizersEvents(organizer.url)
    }
  }

  def organizerOfEvent(eventUrl: String): Organizer = {
    val urlList = List(eventUrl)
    (TicketTailor.fetchOrganizers(urlList) ++ EventBrite.fetchOrganizers(urlList)).head
  }
}

object UpdateOrganizersFromFile extends App with Repositories with Common {
  Events.allOrganizers.foreach(organizerRepository.upsert)
  println(Events.allOrganizers.mkString("\n"))
}

object UpdateEvents extends App with Repositories with Common {
  private lazy val events = Await.result(
    organizerRepository.find(Document({
      "{ deleted: { $not: { $eq: true } }}"
    })).toFuture(), Duration.Inf
  ).flatMap(eventsOf)

  println(events.mkString("\n"))

  filter(events).foreach(eventRepository.upsert)

  def filter(events: Seq[Event]): Seq[Event] = {
    val excludeAdrressWords = List("Bristol", "BN1 1UB").map(_.toLowerCase())
    events.filter(event => !excludeAdrressWords.exists(word => event.address.toLowerCase().contains(word)))
  }
}

object UpdateExistingEvents extends App with Repositories with Common {
  private lazy val events = Await.result(eventRepository.all.toFuture(), Duration.Inf)
  events.filter(_.organizerUrl == "https://www.tickettailor.com/events/dancelondon").par
    .map(event => event.copy(organizerUrl = organizerOfEvent(event.url).url))
    .foreach(eventRepository.replace)
}

object InsertOrganizer extends App with Repositories with Common {
  val organizer = organizerOfEvent("https://www.eventbrite.co.uk/o/studio-108-34126014551")
  organizerRepository.upsert(organizer)

  eventsOf(organizer).foreach(eventRepository.upsert)
}
