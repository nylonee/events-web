package db

import ai.snips.bsonmacros.DatabaseContext
import net.pawel.events.domain.{Event, Organizer, OrganizerType}
import net.pawel.events.{EventBrite, TicketTailor}
import org.mongodb.scala.bson.collection.immutable.Document
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Mode}

import java.io.File
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
}

object UpdateEvents extends App with Repositories with Common {
  private lazy val events = Await.result(organizerRepository.find(Document({"{ deleted: { $not: { $eq: true } }}"})).toFuture(), Duration.Inf)
    .flatMap(eventsOf)

//  eventRepository
//    .find(Document("organizerUrl" -> "https://www.eventbrite.com/o/the-tantra-institute-14144505274"))
//    .toFuture()
//    .map(_.mkString("\n"))
//    .foreach(eventRepository.)

  println(events.mkString("\n"))
  events.foreach(eventRepository.upsert)
}

object InsertOrganizer extends App with Repositories with Common {

  def makeOrganizer(url: String): Organizer = {
    val urlList = List(url)
    (TicketTailor.fetchOrganizers(urlList) ++ EventBrite.fetchOrganizers(urlList)).head
  }

  val organizer = makeOrganizer("https://www.eventbrite.co.uk/o/hulya-erbeyli-coaching-39859153553")
  organizerRepository.upsert(organizer)

  eventsOf(organizer).foreach(eventRepository.upsert)

}
