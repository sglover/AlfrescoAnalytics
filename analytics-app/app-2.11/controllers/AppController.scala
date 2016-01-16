package controllers

import javax.inject.Inject
import org.alfresco.analytics.client.Register
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.json._
import play.api.mvc.{Controller, WebSocket}
import play.libs.Akka

//@Singleton
class AppController @Inject() extends Controller {
//object AppController extends Controller {
//  def index = {
//    Action(request => Ok(views.html.app.index()))
//  }

    val websockets = Akka.system().actorSelection("/user/websockets")
  
    def eventsWS = WebSocket.using[JsValue] {
      request =>
        val (out, channel) = Concurrent.broadcast[JsValue]
        val in = Iteratee.foreach[JsValue] { json =>
          val username = (json \ "username").asOpt[String]
          username match {
            case Some(username) => {
              websockets ! Register(username, channel)
            }
            case None => {
                // Just ignore the input
//                val in = Iteratee.ignore[JsValue]
            }
          }
        }

        (in, out)

//        request.headers.get("username") match {
//            case Some(username) => {
//              val (out, channel) = Concurrent.broadcast[JsValue]
//              eventProcessor ! Register(username, channel)
//              val in = Iteratee.ignore[JsValue]
//              (in, out)
//            }
//            case None => {
//                // Just ignore the input
//                val in = Iteratee.ignore[JsValue]
//
//                // Send a single 'Hello!' message and close
//                val out = Enumerator(Json.toJson(-1)).andThen(Enumerator.eof)
//                (in, out)
//            }
//        }
    }

//    def indexWS = WebSocket.using[JsValue] {
//      request =>
//
////   def indexWS = withAuthWS {
////     userId =>
//
//        request.headers.get("username") match {
//            case Some(username) => {
//              val (out, channel) = Concurrent.broadcast[JsValue]
//              val cacheActor = Akka.system.actorOf(CacheActor.props(channel, username, cacheServer))
//              val in = Iteratee.foreach[JsValue] {
//                  msg => cacheActor ! Message(msg)
//              }
//              (in, out)
//            }
//            case None => {
//                // Just ignore the input
//                val in = Iteratee.ignore[JsValue]
//                
//                // Send a single 'Hello!' message and close
//                val out = Enumerator(Json.parse("{\"error\":\"Must supply a username header\"}")).andThen(Enumerator.eof)
//                (in, out)
//            }
//        }
//    }
}
