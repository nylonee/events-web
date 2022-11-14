package db

import ai.snips.bsonmacros.DatabaseContext
import net.pawel.events.domain.OrganizerType
import net.pawel.events.{EventBrite, TicketTailor}
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Mode}

import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object UpdateEvents extends App {

  import play.api.ConfigLoader._

  private val configuration = Configuration
    .load(Environment(new File("."), Configuration.getClass.getClassLoader, Mode.Dev))
  println(configuration.get[String]("mongodb.uri"))

  private val lifecycle = new ApplicationLifecycle {
    override def addStopHook(hook: () => Future[_]): Unit = {}

    override def stop(): Future[_] = Future.successful({})
  }
  private val context = new DatabaseContext(configuration, lifecycle)
  private val organizerRepository = new OrganizerRepository(context)
  private val eventRepository = new EventRepository(context)

  private val events = Await.result(organizerRepository.all.toFuture(), Duration.Inf)
    .flatMap(organizer => organizer.organizerType match {
      case OrganizerType.TicketTailor => TicketTailor.fetchOrganizerEvents(organizer.url)
      case OrganizerType.EventBrite => EventBrite.organizersEvents(organizer.url)
    })

  println(events.mkString("\n"))
  events.foreach(eventRepository.upsert)
}
