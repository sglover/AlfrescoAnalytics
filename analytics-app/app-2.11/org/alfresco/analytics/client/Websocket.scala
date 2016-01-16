package org.alfresco.analytics.client

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages.KafkaMessage
import com.softwaremill.react.kafka.{ConsumerProperties, ReactiveKafka}
import kafka.serializer.StringDecoder
import org.alfresco.events.types._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json._
//import spray.json._
import scala.concurrent.duration._ // if you don't supply your own Protocol (see below)

/**
  * Created by sglover on 04/12/2015.
  */
class Websocket extends Actor with ActorLogging {
  val channels:collection.mutable.Map[String, Channel[JsValue]] = collection.mutable.Map()

  implicit val actorSytem = context.system

  println("websocket1")
  val kafka = new ReactiveKafka()

  val activitiesConsumerProperties = ConsumerProperties(
    brokerList = "localhost:9092",
    zooKeeperHost = "localhost:2181",
    topic = "watched.activities",
    groupId = "group100",
    decoder = new StringDecoder()
  )
  .commitInterval(5 seconds) // flush interval
  .kafkaOffsetsStorage()
  .readFromEndOfStream()

  val analyticsConsumerProperties = ConsumerProperties(
    brokerList = "localhost:9092",
    zooKeeperHost = "localhost:2181",
    topic = "watched.analytics",
    groupId = "group100",
    decoder = new StringDecoder()
  )
  .commitInterval(5 seconds) // flush interval
  .kafkaOffsetsStorage()
  .readFromEndOfStream()

  def processActivitiesMessage(msg: KafkaMessage[String]) = {
//    val event = Serializer.deserializeAsEvent(msg.message())
    val event = msg.message()
    println("event" + event)
    val json: JsValue = Json.obj(
                  "msg" -> event
                )

//    event match {
//      case e:ActivityEvent => {
//        println("activity event" + e)
//        for(channel <- channels.values) {
//          val json: JsValue = JsObject(Seq(
//            "nodeId" -> JsString(e.getNodeId()),
//            "path" -> JsString(e.getActivityData()),
//            "type" -> JsString(e.getType())
//          ))
//          channel push(json)
//        }
//      }
//    }
    for(channel <- channels.values) {
      channel push json
    }

    msg
  }

  def processAnalyticsMessage(msg: KafkaMessage[String]) = {
    val message = msg.message()
    for(channel <- channels.values) {
      channel push Json.parse(message)
    }

    msg
  }

//    val event = Json.fromJson[AnalyticsEvent]
//    println("event" + event)
//
//    event match {
//      case e:PopularEvent => {
//        println("popular event" + e)
//        for(channel <- channels.values) {
//          val json: JsValue = Json.obj(
//            "events" -> Json.arr(e.l.map(x => Json.obj("count" -> JsNumber(x.count),
//                                                      "nodeId" -> JsString(x.nodeid),
//                                                      "ts" -> JsNumber(x.timestamp),
//                                                      "yearmonth" -> JsString(x.yearmonth))))
//          )
//          channel push json
//        }
//      }
//      case something => {
//        println(s"ignored $something")
//      }
//    }

//    val json: JsValue = Json.obj(
//      "msg" -> event
//    )

//    val json = msg.message().parseJson

//    val event = Serializer.deserializeAsAnalyticsEvent(msg.message())
//    println("event" + event)
//    event match {
//      case e:PopularEvent => {
//        println("popular event" + e)
//        for(channel <- channels.values) {
//          val json: JsValue = Json.obj(
//            "events" -> Json.arr(e.l.map(x => Json.obj("count" -> JsNumber(x.count),
//                                                      "nodeId" -> JsString(x.nodeid),
//                                                      "ts" -> JsNumber(x.timestamp),
//                                                      "yearmonth" -> JsString(x.yearmonth))))
//          )
//          channel push json
//        }
//      }
//      case something => {
//        println(s"ignored $something")
//      }
//    }

//    for(channel <- channels.values) {
//      channel push json
//    }

//    msg
//  }

  implicit val materializer = ActorMaterializer()

  val activitiesConsumer = kafka.consumeWithOffsetSink(activitiesConsumerProperties)
  Source(activitiesConsumer.publisher)
    .map(processActivitiesMessage(_)) // your message processing
    .to(activitiesConsumer.offsetCommitSink) // stream back for commit
    .run()

  val analyticsConsumer = kafka.consumeWithOffsetSink(analyticsConsumerProperties)
  Source(analyticsConsumer.publisher)
    .map(processAnalyticsMessage(_)) // your message processing
    .to(analyticsConsumer.offsetCommitSink) // stream back for commit
    .run()

  sys.addShutdownHook {
    activitiesConsumer.cancel()
    analyticsConsumer.cancel()
  }


//  sendIdentifyRequest()
//
//  def sendIdentifyRequest(): Unit = {
//    context.actorSelection("/user/kafka") ! Identify("/user/kafka")
//    import context.dispatcher
//    context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
//  }
//
//  def receive = identifying
//
//  def identifying: Actor.Receive = {
//    case ActorIdentity(path, Some(actor)) =>
//      println("Response from remote entities actor")
//      context.watch(actor)
//      actor ! RegisterWebsocket(context.self)
//      context.become(active(actor))
//    case ActorIdentity(path, None) => println(s"Remote actor not available: $path")
//    case ReceiveTimeout            => sendIdentifyRequest()
//    case _                         => println("Not ready yet")
//  }
//
//  def active(kafkaActor: ActorRef) : Actor.Receive = {

  def receive = {
    case Register(username:String, channel:Channel[JsValue]) => {
      channels += username -> channel
      println("Registered" + username)
    }
    case e:ActivityEvent => {
      println("repoevent" + e)
      for(channel <- channels.values) {
        val json: JsValue = JsObject(Seq(
          "nodeId" -> JsString(e.getNodeId()),
          "path" -> JsString(e.getActivityData()),
          "type" -> JsString(e.getType())
        ))
        channel push(json)
      }
    }
    case default => println("Skipped event " + default)
  }

//  def receive = {
//    case Register(username:String, channel:Channel[JsValue]) => {
//      channels += username -> channel
//      println("Registered" + username)
//    }
////    case RepoEvent(ne:NodeEvent) => {
//    case RepoEvent(e:Event) => {
//      println("repoevent" + e)
//      e match {
//        case ne:NodeEvent => {
//          for(channel <- channels.values) {
//            val json: JsValue = JsObject(Seq(
//              "nodeId" -> JsString(ne.getNodeId()),
//              "path" -> JsString(ne.getPaths().get(0)),
//              "type" -> JsString(ne.getType())
//            ))
//            channel push(json)
//          }
//        }
//        case ae:ActivityEvent => {
//          for(channel <- channels.values) {
//            val json: JsValue = JsObject(Seq(
//              "type" -> JsString(ae.getType()),
//              "nodeId" -> JsString(ae.getNodeId()),
//              "username" -> JsString(ae.getUsername())
//            ))
//            channel push(json)
//          }
//        }
//        case something =>
//      }
//    }
//    case _ =>
//  }
}
