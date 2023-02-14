package db

import ai.snips.bsonmacros.DatabaseContext
import net.pawel.events.Utils.{await, parallelize}
import net.pawel.events.domain.{Event, Organizer, OrganizerType}
import net.pawel.events.{Dandelion, EventBrite, Events, TicketTailor}
import org.mongodb.scala.bson.collection.immutable.Document
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Mode}

import java.io.File
import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait Repositories {

  private lazy val configuration = Configuration
    .load(Environment(new File("."), Configuration.getClass.getClassLoader, Mode.Dev))

  private lazy val lifecycle = new ApplicationLifecycle {
    override def addStopHook(hook: () => Future[_]): Unit = {}

    override def stop(): Future[_] = Future.successful({})
  }
  private lazy val context = new DatabaseContext(configuration, lifecycle)
  lazy val organizerRepository = new OrganizerRepository(context)
  lazy val eventRepository = new EventRepository(context)
}

trait Common {
  def eventsOf(organizer: Organizer): List[Event] = {
    println("Fetching events for " + organizer.name + " " + organizer.organizerType)
    (organizer.organizerType match {
      case OrganizerType.TicketTailor => TicketTailor.fetchOrganizerEvents(organizer.url)
      case OrganizerType.EventBrite => EventBrite.organizersEvents(organizer.url)
      case OrganizerType.Dandelion => Dandelion.organizersToEventsMap(organizer.url)
    }).filter(filterEvent)
  }

  private def filterEvent(event: Event): Boolean = {
    val excludeAdrressWords = List("Bristol", "BN1 1UB").map(_.toLowerCase())
    !excludeAdrressWords.exists(word => event.address.toLowerCase().contains(word))
  }

  def organizerOfEvent(eventUrl: String): Organizer = {
    val urlList = parallelize(List(eventUrl))
    (TicketTailor.fetchOrganizers(urlList) ++ EventBrite.fetchOrganizers(urlList)).head
  }
}

object UpdateOrganizersFromFile extends App with Repositories with Common {
  Events.allOrganizers.foreach(organizerRepository.upsert)
  println(Events.allOrganizers.mkString("\n"))
}

object UpdateEvents extends App with Repositories with Common {
  private val organizersFuture = organizerRepository.find(Document({
    "{ deleted: { $not: { $eq: true } }}"
  })).toFuture()

  val updateDandelionOrganizers =
    for {
      organizers <- organizersFuture
      organizersMap = organizers.map(organizer => organizer.url -> organizer).toMap
      dandelionOrganizers <- Future { Dandelion.organizers }
      filtered = dandelionOrganizers.filterNot(org => organizersMap.isDefinedAt(org.url))
      _ <- Future.sequence(filtered.map(organizerRepository.upsert))
    } yield {
      println("All Dandelion organizers:\n" + dandelionOrganizers.mkString("\n"))
      println("New Dandelion organizers:\n" + filtered.mkString("\n"))
      println("Updated organizers")
      ()
    }

  val eventsFuture = for {
    organizers <- organizersFuture
    events <- Future { parallelize(organizers).flatMap(eventsOf).toList }
    _ <- updateDandelionOrganizers
  } yield {
    println("Main for finished")
    events
  }

  private def keyFor(event: Event): String =
    event.url + event.start.getEpochSecond

  lazy val events = await(eventsFuture)
  val existingEvents = await(eventRepository.all.toFuture())
  val existingSet = existingEvents.map(event => keyFor(event)).toSet

  val newEvents = events.filterNot(event => existingSet.contains(keyFor(event)))

  println("All events:\n" + events.mkString("\n"))

  println("Events to be inserted:\n" + newEvents.mkString("\n"))

  await(Future.sequence(newEvents.map(eventRepository.upsert)))
  println("New events were inserted")
}

object UpdateExistingEvents extends App with Repositories with Common {
  private lazy val events = Await.result(eventRepository.all.toFuture(), Duration.Inf)
  events.filter(_.organizerUrl == "").par
    .map(event => event.copy(organizerUrl = organizerOfEvent(event.url).url))
    .foreach(eventRepository.replace)
}

object InsertOrganizer extends App with Repositories with Common {
  val organizer = organizerOfEvent("")
  organizerRepository.upsert(organizer)

  val events = eventsOf(organizer)
  println(events.mkString("\n"))
  events.foreach(eventRepository.upsert)
}