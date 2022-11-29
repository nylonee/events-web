package controllers

import db.EventRepository
import net.pawel.events.domain.Event
import play.api.libs.json.{JsArray, JsObject, JsString}
import play.api.mvc._

import java.time.temporal.ChronoUnit
import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               val eventRepository: EventRepository) extends BaseController {

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
}





