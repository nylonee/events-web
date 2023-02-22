package db

import ai.snips.bsonmacros.DatabaseContext
import kong.unirest.Unirest
import net.pawel.events.util.Utils.{await, parallelize}
import net.pawel.events.domain.{Event, Organizer, OrganizerType}
import net.pawel.events.util.PathFromUrl.makePathFromUrl
import net.pawel.events.{Dandelion, EventBrite, Events, TicketTailor}
import org.mongodb.scala.bson.collection.immutable.Document
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Mode}

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
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
  lazy val ticketTailor = new TicketTailor()
  lazy val eventBrite = new EventBrite()
  lazy val dandelion = new Dandelion()

  def eventsOf(organizer: Organizer): List[Event] = {
    println("Fetching events for " + organizer.name + " " + organizer.organizerType)
    (organizer.organizerType match {
      case OrganizerType.TicketTailor => ticketTailor.fetchOrganizerEvents(organizer.url)
      case OrganizerType.EventBrite => eventBrite.organizersEvents(organizer.url)
      case OrganizerType.Dandelion => dandelion.organizersToEventsMap.get(organizer.url).getOrElse(Nil)
    }).filter(filterEvent)
  }

  private def filterEvent(event: Event): Boolean = {
    val excludeAdrressWords = List("Bristol", "BN1 1UB").map(_.toLowerCase())
    !excludeAdrressWords.exists(word => event.address.toLowerCase().contains(word))
  }

  def organizerOfEvent(eventUrl: String): Organizer = {
    val urlList = parallelize(List(eventUrl))
    (ticketTailor.fetchOrganizers(urlList) ++ eventBrite.fetchOrganizers(urlList)).head
  }
}

object UpdateOrganizersFromFile extends App with Repositories with Common {
  Events.allOrganizers.foreach(organizerRepository.upsert)
  println(Events.allOrganizers.mkString("\n"))
}

object UpdateEvents extends App with Repositories with Common {
  lazy val organizersFuture = organizerRepository.find(Document({
    "{ deleted: { $not: { $eq: true } }}"
  })).toFuture().map(_.filter(_.url == "https://www.tickettailor.com/events/acaringplace"))
  lazy val allOrganizersFuture = organizerRepository.all.toFuture()


  val insertedDandelionOrganizersFuture =
    for {
      organizers <- allOrganizersFuture
      existingDandelionOrganizers = organizers.filter(_.organizerType == OrganizerType.Dandelion)
      organizersSet = existingDandelionOrganizers.map(_.url).toSet
      dandelionOrganizers = dandelion.organizers
      newOrganizers = dandelionOrganizers.filterNot(org => organizersSet.contains(org.url))
      _ <- Future.sequence(newOrganizers.map(organizerRepository.upsert))
    } yield {
      println("All Dandelion organizers:\n" + dandelionOrganizers.mkString("\n"))
      println("New Dandelion organizers:\n" + newOrganizers.mkString("\n"))
      println("Updated organizers")
      newOrganizers
    }

  def eventsForOrganizers(organizersFuture: Future[Seq[Organizer]]): Future[List[Event]] =
    for {
      organizers <- organizersFuture
      events = parallelize(organizers).flatMap(eventsOf).toList
    } yield events

  val eventsFuture = eventsForOrganizers(organizersFuture)
  val newDandelionEventsFuture = eventsForOrganizers(insertedDandelionOrganizersFuture)

  val allEventsFuture = for {
    events <- eventsFuture
    dandelionEvents <- newDandelionEventsFuture
  } yield {
    println("Main for finished")
    events ++ dandelionEvents.filter(onlyLondonEvents)
  }

  def onlyLondonEvents(event: Event): Boolean = {
    val address = event.address.toLowerCase
    (address.contains("london") ||
      address == "online") && !address.contains("canada")
  }

  lazy val events = await(allEventsFuture)

  println("Events:\n" + events.mkString("\n"))

  val inserts = Future.sequence(
    parallelize(events).groupBy(_.organizerUrl).map {
      case (organizerUrl, events) => for {
        _ <- eventRepository.deleteAllOfOrganizer(organizerUrl)
        _ <- Future.sequence(events.map(eventRepository.upsert).toList)
      } yield ()
    }.toList
  )

  await(inserts)
  println("New events were inserted")
}

object UpdateExistingEvents extends App with Repositories with Common {
  private lazy val events = Await.result(eventRepository.all.toFuture(), Duration.Inf)
  events.filter(_.organizerUrl == "https://www.tickettailor.com/events/acaringplace").par
    .map(event => event.copy(organizerUrl = organizerOfEvent(event.url).url))
    .foreach(eventRepository.replace)
}

object InsertOrganizer extends App with Repositories with Common {
  val organizer = organizerOfEvent("https://www.tickettailor.com/events/rebeltantra/827897/")
  organizerRepository.upsert(organizer)

  val events = eventsOf(organizer)
  println(events.mkString("\n"))
  await(Future.sequence(events.map(eventRepository.upsert)))
  println("Events upserted")
}

object EventsOfAnOrganizer extends App with Repositories with Common {
  val events = eventsOf(Organizer("https://www.tickettailor.com/events/acaringplace", "", OrganizerType.TicketTailor))
  println(events.mkString("\n"))
}