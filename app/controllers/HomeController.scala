package controllers

import db.{EventRepository, OrganizerRepository}
import net.pawel.events.domain.{Event, Organizer, OrganizerType}
import net.pawel.events.{EventBrite, Events, TicketTailor}
import play.api.libs.json.{JsArray, JsObject, JsString}
import play.api.mvc._
import play.mvc.Results.ok

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               val eventRepository: EventRepository) extends BaseController {

  val eventList: List[Event] = Nil

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
    //    Ok("")
  }

  def events(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    eventRepository
      .all
      .toFuture
      .map(_.filterNot(event => event.end.minusDays(3).isAfter(event.start))
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
}





