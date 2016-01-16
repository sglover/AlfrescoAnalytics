package org.alfresco.analytics.client

import akka.actor.Props
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.libs.Akka

/**
 * @author sglover
 */

object Actors {
   def createActors(): Unit = {
    println("--camel--Start--")

     //akka.tcp://analytics@127.0.0.1:2552
    val websockets = Akka.system().actorOf(Props(classOf[Websocket]), name = "websockets")
    val kafka = Akka.system().actorOf(Props(classOf[KafkaActor]), name = "kafka")

//    val nodeEventsConsumer = Akka.system().actorOf(Props(classOf[NodeEventsConsumer], websockets), name = "nodeEventsConsumer")
     //val activityEventsConsumer = Akka.system().actorOf(Props(classOf[ActivityEventsConsumer], websockets), name = "activityEventsConsumer")

     /*
  the consumer will have 4 streams and max 64 messages per stream in flight, for a total of 256
  concurrently processed messages.
  */

//     val activityEventsConsumer = Akka.system().actorOf(Props(classOf[KafkaActor]), name = "activityEventsConsumer")

//   val consumerProps = AkkaConsumerProps.forSystem(
//     system = Akka.system(),
//     zkConnect = "localhost:2181",
//     topic = "alfresco.repo.activities",
////     clientId = "analytics",
//     group = "group1",
//     streams = 1, //one per partition
//     keyDecoder = new StringDecoder(),
//     msgDecoder = new StringDecoder(),
//     receiver = activityEventsConsumer,
//     commitConfig = CommitConfig(commitInterval = Option(2 seconds), commitTimeout = Timeout(2 seconds))
//   )

//   val consumer = new AkkaConsumer(consumerProps)
//
//   consumer.start()  //returns a Future[Unit] that completes when the connector is started
//   consumer.commit() //returns a Future[Unit] that completes when all in-flight messages are processed and offsets are committed.
//
//    println("--camel--End--")
//
//     sys addShutdownHook {
//       Await.ready({
//         consumer.stop()
//       }, 2 seconds)   //returns a Future[Unit] that completes when the connector is stopped.
//     }
  }
}

case class Register(username:String, channel:Channel[JsValue])