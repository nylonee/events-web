package controllers

import net.pawel.events.Events

import javax.inject._
import play.api._
import play.api.libs.json.{JsArray, JsObject, JsString}
import play.api.mvc._
import play.libs.Json
import play.mvc.Results.ok

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  val eventList = Events.allEvents

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def events(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val json = JsArray(eventList.map(event => JsObject(List(
      "name" -> JsString(event.name),
      "link" -> JsString(event.link),
      "address" -> JsString(event.address),
      "start" -> JsString(event.start.toString),
      "end" -> JsString(event.end.toString)
    ))))
    Ok(json)
  }

  import play.mvc.Http
  import play.routing.JavaScriptReverseRouter

  def javascriptRoutes(request: Http.Request): play.mvc.Result = {
    ok(JavaScriptReverseRouter.create(
      "jsRoutes",
      "jQuery.ajax",
      request.host,
      routes.javascript.HomeController.events)
    ).as(Http.MimeTypes.JAVASCRIPT)
  }
}
