package controllers

import db.{Common, EventRepository, OrganizerRepository}
import net.pawel.events.util.Utils
import play.api.libs.json.{JsArray, JsObject, JsString}
import play.api.mvc._

import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import java.time.format.DateTimeFormatter

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               val eventRepository: EventRepository,
                               val organizerRepository: OrganizerRepository) extends BaseController with Common {

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
//        Ok("")
  }

  def events(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    eventRepository
      .all
      .toFuture()
      .map(_.filterNot(event => event.end.minus(3, ChronoUnit.DAYS).isAfter(event.start))
        .sortBy(_.start)
        .map(event => JsObject(List(
          "name" -> JsString(event.name),
          "link" -> JsString(event.url),
          "address" -> JsString(event.address),
          "start" -> JsString(event.start.toString),
          "end" -> JsString(event.end.toString)
        ))))
      .map(JsArray(_))
      .map(Ok(_))
  }

  def ics(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    eventRepository
      .all
      .toFuture()
      .map { events =>
        val sb = new StringBuilder
        sb.append("BEGIN:VCALENDAR\n")
        sb.append("VERSION:2.0\n")
        sb.append("PRODID:-//Events Calendar//NONSGML v1.0//EN\n")
        sb.append("CALSCALE:GREGORIAN\n")

        events
          .filterNot(event => event.end.minus(3, ChronoUnit.DAYS).isAfter(event.start))
          .sortBy(_.start)
          .foreach { event =>
            sb.append("BEGIN:VEVENT\n")
            sb.append("UID:" + event.url + "\n")
            sb.append("DTSTART:" + event.start.atZone(ZoneId.of("Z")).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")) + "\n")
            sb.append("DTEND:" + event.end.atZone(ZoneId.of("Z")).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")) + "\n")
            sb.append("SUMMARY:" + event.name + "\n")
            sb.append("LOCATION:" + event.address + "\n")
            sb.append("URL:" + event.url + "\n")
            sb.append("END:VEVENT\n")
          }

        sb.append("END:VCALENDAR")
        sb.toString()
      }
      .map(content => Ok(content).as("text/calendar"))
  }

  def updateCalendar() = Action { request =>

    // Clear all previous events first
    organizerRepository.deleteAll()
    eventRepository.deleteAll()

    val json = request.body.asJson.get
    val urls = json.as[List[String]]

    val events = (for {
      url <- urls
      organizer = organizerOfEvent(url)
      _ = organizerRepository.upsert(organizer)
      events = eventsOf(organizer)
    } yield events).flatten

    Utils.await(Future.sequence(events.map(eventRepository.upsert)))

    println(s"Events upserted for: $urls")

    Ok("Calendar updated with new URLs")
  }
}
