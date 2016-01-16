package org.alfresco.analytics.client

import org.alfresco.events.types._

/**
 * @author sglover
 */
object Camel {

//  val camelSystem = CamelExtension(Akka.system())
//  val amqUrl = s"tcp://localhost:61616"
//  camelSystem.context.addComponent("activemq", ActiveMQComponent.activeMQComponent(amqUrl))
//
//  class ActivityEventsConsumer(eventProcessor: ActorRef) extends Consumer with DB {
//    def endpointUri = "activemq:topic:alfresco.repo.events.activities?clientId=nodetest1&durableSubscriptionName=nodetest1"
//
//    val activityByNode = DB.session.prepare(
//      s"INSERT INTO ${DB.keyspace}.activities_by_node (nodeId, type, username, timestamp) VALUES(?, ?, ?, ?);")
//
//    val activityByUsername = DB.session.prepare(
//      s"INSERT INTO ${DB.keyspace}.activities_by_username (nodeId, type, username, timestamp) VALUES(?, ?, ?, ?);")
//
//    def receive: Actor.Receive = {
//      case msg: CamelMessage => {
//        val event: Event = Serializer.deserialize(msg.bodyAs[String])
//        println("activity event=" + event)
//        event match {
//          case ae:ActivityEvent => {
//            DB.session.execute(activityByNode.bind(ae.getNodeId(), ae.getType(), ae.getUsername(),
//                new Date(ae.getTimestamp)))
//            DB.session.execute(activityByUsername.bind(ae.getNodeId(), ae.getType(), ae.getUsername(),
//              new Date(ae.getTimestamp)))
//          }
//          case something =>
//        }
//        eventProcessor ! RepoEvent(event)
//      }
//      case something => {
//        println("something=" + something)
//      }
//    }
//  }
//
//  class NodeEventsConsumer(eventProcessor: ActorRef) extends Consumer {
//    def endpointUri = "activemq:topic:alfresco.repo.events.nodes?clientId=activitytest1&durableSubscriptionName=activitytest1"
//
//    def receive:Actor.Receive = {
//      case msg:CamelMessage => {
//        val event:Event = Serializer.deserialize(msg.bodyAs[String])
//        println("event=" + event)
//        eventProcessor ! RepoEvent(event)
//        event match {
//          case ae:ActivityEvent => {
//            ae.getActivityData
//          }
//          case ce:ContentEventImpl => {
//
//          }
//          case ne:NodeEvent => {
//            println("nodeevent=" + event)
//
////            ne match {
////              case cp: NodeContentPutEvent => {
////                cp.getNodeType() match {
////                  case "cm:content" => {
////                    actor ! cp
////                  }
////                }
////              }
////              case _ =>
////            }
//
////            eventProcessor ! RepoEvent(ne)
//          }
//          case _ =>
//        }
//      }
//      case something => {
//          println("something=" + something)
//      }
//    }
//  }
}

//case class RepoEvent(event:NodeEvent)
case class RepoEvent(event:Event)