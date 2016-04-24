package org.alfresco.analytics.client

import java.io.{File, FileInputStream, FileOutputStream, InputStreamReader}
import java.util.{Date, Properties}

import _root_.edu.stanford.nlp.ling.CoreAnnotations.{CharacterOffsetBeginAnnotation, CharacterOffsetEndAnnotation, NamedEntityTagAnnotation, PartOfSpeechAnnotation, TextAnnotation, TokensAnnotation}
import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, OneForOneStrategy, SupervisorStrategy}
import akka.stream.scaladsl.{Sink, Source}
import com.sclasen.akka.kafka.StreamFSM
import com.softwaremill.react.kafka.KafkaMessages.{StringProducerMessage, StringConsumerRecord}
import com.softwaremill.react.kafka.{ProducerProperties, ProducerMessage, ConsumerProperties, ReactiveKafka}
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import kafka.serializer.StringDecoder
import org.alfresco.analytics._
import org.alfresco.events.types.{ActivityEvent, Event}
import org.alfresco.transformation.api.{MimeType, TransformRequest, TransformResponse}
import org.alfresco.transformation.client.TransformationCallback
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl
import org.apache.commons.io.IOUtils
import org.apache.kafka.common.serialization.{StringSerializer, StringDeserializer}
import org.joda.time.DateTime
import org.reactivestreams.{Subscriber, Publisher}
import play.api.libs.json._

import scala.collection.JavaConverters._
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.global

/**
  * Created by sglover on 18/12/2015.
  */
class KafkaActor extends Actor with DB with CMISSession {

  lazy val repoUsername = "admin"
  lazy val repoPassword = "admin"

  val activityByNode = DB.session.prepare(
    s"INSERT INTO ${DB.keyspace}.activities_by_node (nodeId, type, username, timestamp) VALUES(?, ?, ?, ?);")

  val activityByUsername = DB.session.prepare(
    s"INSERT INTO ${DB.keyspace}.activities_by_username (nodeId, type, username, timestamp) VALUES(?, ?, ?, ?);")

  val entitiesByNode = DB.session.prepare(
    s"INSERT INTO ${DB.keyspace}.entities_by_node (nodeId, nodeVersion, timestamp, entityType, entity, beginOffset, endOffset, probability, context, count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")

  val entitiesByType = DB.session.prepare(
    s"INSERT INTO ${DB.keyspace}.entities_by_type (nodeId, nodeVersion, timestamp, entityType, entity, beginOffset, endOffset, probability, context, count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")

  override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case exception => Resume // Your custom error handling
  }

  println("KafkaActor")

  lazy val t = new Transform()

  implicit val actorSytem = context.system

  val kafka = new ReactiveKafka()

  object MyCoreNLP {
    val props = new Properties()
    props.put("annotators", "tokenize, ssplit, pos, lemma, ner")
    props.put("pos.maxlen", Int.box(300))
    props.put("useNGrams", Boolean.box(true))
    props.put("maxNGramLeng", Int.box(10))
    @transient lazy val pipeline = new StanfordCoreNLP(props)
  }

  def getEntities(nodeId:String, f:File):Unit = {

    val in = new InputStreamReader(new FileInputStream(f), "UTF-8")

    val buf = new Array[Char](1024*500)
    IOUtils.readFully(in, buf)
    val s = new String(buf)

    // create an empty Annotation just with the given text
    val document = new Annotation(s)

    // run all Annotators on this text
    MyCoreNLP.pipeline.annotate(document)

    // these are all the sentences in this document
    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
    document
      .get(classOf[TokensAnnotation])
      .asScala.toList
      .map { token => {
        // this is the text of the token
        val word = token.get(classOf[TextAnnotation])
        // this is the POS tag of the token
        val pos = token.get(classOf[PartOfSpeechAnnotation])
        // this is the NER label of the token
        val ne: String = token.get(classOf[NamedEntityTagAnnotation])
        val beginOffset: Int = token.get(classOf[CharacterOffsetBeginAnnotation])
        val endOffset: Int = token.get(classOf[CharacterOffsetEndAnnotation])
        val context: String = ""
        val timestamp = DateTime.now().toDate()

        ne match {
          case "LOCATION" => {
            Some(Entity(nodeId, -1, timestamp, "location", word, beginOffset, endOffset, 1.0, context))
          }
          case "PERSON" => {
            Some(Entity(nodeId, -1, timestamp, "person", word, beginOffset, endOffset, 1.0, context))
          }
          case "ORGANIZATION" => {
            Some(Entity(nodeId, -1, timestamp, "org", word, beginOffset, endOffset, 1.0, context))
          }
          case "MISC" => {
            Some(Entity(nodeId, -1, timestamp, "misc", word, beginOffset, endOffset, 1.0, context))
          }
          //          case _ => {
          //            None
          //          }
        }
      }
      }
      .foreach {
        x => {
          x match {
            case Some(e) => {
              DB.session.execute(entitiesByType.bind(e.nodeid))
//              DB.session.execute(entitiesByType.bind(e.nodeid, e.nodeversion, e.timestamp,
//                e.entitytype, e.beginOffset, e.endOffset, e.probability, e.context, e.count))
//              DB.session.execute(entitiesByNode.bind(e.nodeid, e.nodeversion, e.timestamp,
//                e.entitytype, e.beginOffset, e.endOffset, e.probability, e.context, e.count))
            }
          }
        }
      }
  }

//  def convert(nodeId:String, mimeType:MimeType, nodePath:String): Future[String] = {
//    val objectId = new ObjectIdImpl(nodeId)
//
//    println(s"Content update $nodeId $mimeType $nodePath")
//
//    val temp = TempDirectory.createTemporaryFile("kafka")
//    temp.deleteOnExit()
//    println(s"Created temporary file $temp")
//    val path = temp.getAbsolutePath()
//    val os = new FileOutputStream(temp)
//
//    println(s"Getting binary content for $nodeId $mimeType $path")
//
//    val cs = cmisSession.getContentStream(objectId)
//    IOUtils.copy(cs.getStream(), os)
//
//    println(s"Got binary content for $nodeId $mimeType $path")
//
//    val p = Promise[String]
//    val f = p.future
//
//    Future {
//      val c = new C(p)
//      t.sendTransform(path, mimeType, c)
////      ProducerMessage(c.getPath())
//    }
//
//    f
//  }

//  class C(p:Promise[String]) extends TransformationCallback {
//    override def onError(r:TransformRequest, t:Throwable): Unit = {
//      p failure t
//    }
//
//    override def transformCompleted(transformResponse: TransformResponse): Unit = {
//      val textPath = transformResponse.getTargets.get(0).getPath()
//      println(s"textPath=$textPath")
//      p success textPath
//    }
//  }

  val extractNodeIdFolder = """folder-details?nodeRef=workspace%3A%2F%2FSpacesStore%2F(.+)""".r
  val extractNodeId = """document-details\?nodeRef=workspace%3A%2F%2FSpacesStore%2F(.+)""".r

  //: Option[String]
  def getNodeId(js:JsValue) = {
    (js \ "nodeId").validate[String] match {
      case e:JsError => {
        (js \ "activityData" \ "page").validate[String] match {
          case s: JsSuccess[String] => {
            s.get match {
              case extractNodeId(nodeId) => Some(nodeId)
              case extractNodeIdFolder(nodeId) => Some(nodeId)
              case _ => None
            }
          }
          case e: JsError => None
        }
      }
      case nodeId: JsSuccess[String] => {
        Some(nodeId)
      }
    }
  }

  def getMimeType(js:JsValue): Option[MimeType] = {
    (js \ "mimeType").validate[String] match {
      case e:JsError => None
      case mimeType:JsSuccess[String] => {
        Some(MimeType.INSTANCES.getByMimetype(mimeType.get))
      }
    }
  }

  def getNodePath(js:JsValue): Option[String] = {
    val nodePaths = (js \ "paths").validate[List[String]]
    nodePaths match {
      case e:JsError => None
      case paths:JsSuccess[List[String]] => {
        paths.get.size match {
          case 0 => None
          case _ => Some(paths.get(0))
        }
      }
    }
  }

  def processActivitiesMessage(event: StringConsumerRecord): Option[StringProducerMessage] = {
//    val event = msg.message()
    println("event" + event)
    val json: JsValue = Json.obj(
      "msg" -> event.value()
    )

    (json \ "@class").validate[String] match {
      case x:JsSuccess[String] => {
        x.get match {
          case "org.alfresco.events.types.ActivityEvent" => {
            (json \ "type").validate[String] match {
              case y:JsSuccess[String] => {
                y.get match {
                  case "activity.org.alfresco.documentlibrary.file-updated" => {
                    val nodePath = getNodePath(json).getOrElse("")
                    getNodeId(json) match {
                      case Some(nodeId) => {
                        getMimeType(json) match {
                          case Some(mimeType) => {
                            convert(nodeId, mimeType, nodePath) onSuccess {
                              case m => Some(ProducerMessage(m))
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  //val activitiesConsumerProperties =
  val activitiesConsumer: Publisher[StringConsumerRecord] = kafka.consume(
    ConsumerProperties(
      bootstrapServers = "localhost:9092",
      //zooKeeperHost = "localhost:2181",
      topic = "alfresco.repo.activities",
      groupId = "group100",
  //    decoder = new StringDecoder()
      valueDeserializer = new StringDeserializer()
    )
    .commitInterval(5 seconds) // flush interval
    .consumerTimeoutMs(300)
  //  .kafkaOffsetsStorage()
    .readFromEndOfStream()
  )

  val textContentSubscriber: Subscriber[StringProducerMessage] = kafka.publish(ProducerProperties(
    bootstrapServers = "localhost:9092",
    topic = "org.alfresco.textcontent",
    valueSerializer = new StringSerializer()
  ))

//  val activitiesConsumer = kafka.consumeWithOffsetSink(activitiesConsumerProperties)
  Source
    .fromPublisher(activitiesConsumer)
//  Source(activitiesConsumer.publisher)
    .map(processActivitiesMessage(_)) // your message processing
    .to(activitiesConsumer.offsetCommitSink) // stream back for commit
//    .to(Sink.fromSubscriber(subscriber))
    .run()


//  def createSupervisedSubscriberActor() = {
//    val kafka = new ReactiveKafka()
//
//    // subscriber
//    val subscriberProperties = ProducerProperties(
//      brokerList = "localhost:9092",
//      topic = "alfresco.repo.activities",
//      clientId = "analytics",
//      encoder = new StringEncoder
//    )
//    val subscriberActorProps: Props = kafka.producerActorProps(subscriberProperties)
//    context.actorOf(subscriberActorProps)
//  }

  override def receive: Receive = {
    case s:String => {
      val event:Event = Serializer.deserializeAsEvent(s)
      println("kafka activity event=" + event)
      event match {
        case ae:ActivityEvent => {
          DB.session.execute(activityByNode.bind(ae.getNodeId(), ae.getType(), ae.getUsername(),
            new Date(ae.getTimestamp)))
          DB.session.execute(activityByUsername.bind(ae.getNodeId(), ae.getType(), ae.getUsername(),
            new Date(ae.getTimestamp)))
          println("persisted kafka activity event=" + event)
          sender ! StreamFSM.Processed
        }
        case something => {
          sender ! StreamFSM.Processed
        }
      }
    }
    case something => {
      println("KafkaActor:" + something)
      sender ! StreamFSM.Processed
    }
  }
}
