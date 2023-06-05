package db

import ai.snips.bsonmacros.DatabaseContext
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.{Calendar, CalendarScopes, model}
import net.pawel.events.domain.{Event, Organizer, OrganizerType}
import net.pawel.events.util.Utils.{await, parallelize}
import net.pawel.events.{Dandelion, EventBrite, Events, TicketTailor}
import org.mongodb.scala.bson.collection.immutable.Document
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Mode}

import java.io.{File, FileNotFoundException, InputStreamReader}
import java.util
import java.util.Collections
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
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
    try {
      (organizer.organizerType match {
        case OrganizerType.TicketTailor => ticketTailor.fetchOrganizerEvents(organizer.url)
        case OrganizerType.EventBrite => eventBrite.organizersEvents(organizer.url)
        case OrganizerType.Dandelion => dandelion.organizersToEventsMap.get(organizer.url).getOrElse(Nil)
      }).filter(filterEvent)
    } catch {
      case e =>
        println(s"Failed to fetch events for an organizer: ${organizer.url}")
        throw e
    }
  }

  private def filterEvent(event: Event): Boolean = {
    val excludeAdrressWords = List("Bristol", "BN1 1UB").map(_.toLowerCase())
    !excludeAdrressWords.exists(word => event.address.toLowerCase().contains(word))
  }

  def organizerOfEvent(eventUrl: String): Organizer = {
    val urlList = parallelize(List(eventUrl))
    val ticketTailorOrganizer = ticketTailor.fetchOrganizers(urlList)
    val eventBriteOrganizer = eventBrite.fetchOrganizers(urlList)
    (ticketTailorOrganizer ++ eventBriteOrganizer).head
  }
}

object UpdateOrganizersFromFile extends App with Repositories with Common {
  Events.allOrganizers.foreach(organizerRepository.upsert)
  println(Events.allOrganizers.mkString("\n"))
}

object UpdateEvents extends App with Repositories with Common {
  lazy val organizersFuture = organizerRepository.find(Document({
    "{ deleted: { $not: { $eq: true } }}"
  })).toFuture()
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

  def eventsForOrganizers(organizersFuture: Future[Seq[Organizer]]): List[Event] = {
    val organizers = await(organizersFuture)
    parallelize(organizers).flatMap(eventsOf).toList
  }

  def onlyLondonEvents(event: Event): Boolean = {
    val address = event.address.toLowerCase
    (address.contains("london") ||
      address == "online") && !address.contains("canada")
  }

  val events = eventsForOrganizers(organizersFuture)
  val dandelionEvents = eventsForOrganizers(insertedDandelionOrganizersFuture).filter(onlyLondonEvents)

  println("All events gathered")

  lazy val allEvents = events ++ dandelionEvents

  println("Events:\n" + allEvents.mkString("\n"))

  val inserts = Future.sequence(
    parallelize(allEvents).groupBy(_.organizerUrl).map {
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
  events.filter(_.organizerUrl == "https://www.tickettailor.com/events/rebeltantra").par
    .map(event => event.copy(organizerUrl = organizerOfEvent(event.url).url))
    .foreach(eventRepository.replace)
}

object InsertOrganizer extends App with Repositories with Common {
  val organizer = organizerOfEvent("https://www.eventbrite.co.uk/e/daves-improv-playtime-improvisation-drop-in-class-basics-scenework-tickets-643793582937?aff=ebdsoporgprofile")
  organizerRepository.upsert(organizer)

  val events = eventsOf(organizer)
  println(events.mkString("\n"))
  await(Future.sequence(events.map(eventRepository.upsert)))
  println("Events upserted")
}

object EventsOfAnOrganizer extends App with Repositories with Common {
  val events = eventsOf(Organizer("https://www.tickettailor.com/events/dancelondon", "", OrganizerType.TicketTailor))
  println(events.mkString("\n"))
}

object GoogleCalendar extends App {
  private val APPLICATION_NAME = "Google Calendar API Java Quickstart"
  private val JSON_FACTORY = GsonFactory.getDefaultInstance
  private val TOKENS_DIRECTORY_PATH = "tokens"
  private val SCOPES = CalendarScopes.all()
  private val CREDENTIALS_FILE_PATH = "/google_credentials.json"

  private def credentials(httpTransport: NetHttpTransport): Credential = {
    // Load client secrets.
    val in = classOf[NetHttpTransport].getResourceAsStream(CREDENTIALS_FILE_PATH)
    if (in == null) throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH)
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in))
    // Build flow and trigger user authorization request.
    val flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
      .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
      .setAccessType("offline").build
    val receiver = new LocalServerReceiver.Builder().setPort(8888).build
    val credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    //returns an authorized Credential object.
    credential
  }

  val httpTransport = GoogleNetHttpTransport.newTrustedTransport();
  val calendarService =
    new Calendar.Builder(httpTransport, JSON_FACTORY, credentials(httpTransport))
      .setApplicationName(APPLICATION_NAME)
      .build();

  val now = new DateTime(System.currentTimeMillis)

  val calendarToInsert: model.Calendar = new model.Calendar().setId("conscious-events").setSummary("Conscious Events in London")
  val calendar = calendarService.calendars().insert(calendarToInsert).execute()
  println(calendar)
//  val events = calendarService.events.list("primary").setMaxResults(10).setTimeMin(now).setOrderBy("startTime").setSingleEvents(true).execute
//  val items = events.getItems
//  if (items.isEmpty) System.out.println("No upcoming events found.")
//  else {
//    System.out.println("Upcoming events")
//    items.foreach { event =>
//      var start = event.getStart.getDateTime
//      if (start == null) start = event.getStart.getDate
//      System.out.printf("%s (%s)\n", event.getSummary, start)
//    }
//  }
}

object Bla extends App {

}