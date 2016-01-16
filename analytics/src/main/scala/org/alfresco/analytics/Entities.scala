package org.alfresco.analytics

import akka.actor.Actor
import com.datastax.spark.connector.SomeColumns
import com.sclasen.akka.kafka.StreamFSM
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.pipeline.Annotation
import org.alfresco.analytics.MyJsonProtocol._
import org.apache.hadoop.io.{Text, LongWritable}
import org.apache.hadoop.mapred.TextInputFormat
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.joda.time.DateTime
import spray.json._
import com.datastax.spark.connector.{toNamedColumnRef, toRDDFunctions, _}
import scala.collection.JavaConverters._

class Entities(sc:SparkContext, kafkaSink:KafkaSink) extends Actor with DataSelection {
  //with ActorHelper {

//  object Formats extends DefaultJsonProtocol {
//    implicit val NodeAndPathFormat = jsonFormat2(NodeAndPath)
//  }
//  import Formats._

  lazy val t = new Transform(context.system)
  lazy val repoUsername = "admin"
  lazy val repoPassword = "admin"

  override def preStart() = {
    println("")
    println("=== Entities is starting up ===")
    println(s"=== path=${context.self.path} ===")
    println(s"=== Hello ===")
    println("")
  }

  def getEntities(nodeId:String, sentencesRDD:RDD[Sentence]):Unit = {
    println(s"getEntities")
    val namesRDD = sentencesRDD.flatMap(sentence => {

      // create an empty Annotation just with the given text
      val document = new Annotation(sentence.line)

      // run all Annotators on this text
      MyCoreNLP.pipeline.annotate(document)

      // these are all the sentences in this document
      // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
      document
        .get(classOf[TokensAnnotation])
        .asScala.toList
        .flatMap(token => {
          // this is the text of the token
          val word = token.get(classOf[TextAnnotation])
          // this is the POS tag of the token
          val pos = token.get(classOf[PartOfSpeechAnnotation])
          // this is the NER label of the token
          val ne: String = token.get(classOf[NamedEntityTagAnnotation])
          val beginOffset: Long = token.get(classOf[CharacterOffsetBeginAnnotation]) + sentence.offset
          val endOffset: Long = token.get(classOf[CharacterOffsetEndAnnotation]) + sentence.offset
          val context: String = ""
          val timestamp = DateTime.now().toDate()

          println(s"ne=$ne")
          ne match {
            case "LOCATION" => {
              List(Entity(nodeId, -1, timestamp, "location", word, beginOffset, endOffset, 1.0, context))
            }
            case "PERSON" => {
              List(Entity(nodeId, -1, timestamp, "person", word, beginOffset, endOffset, 1.0, context))
            }
            case "ORGANIZATION" => {
              List(Entity(nodeId, -1, timestamp, "org", word, beginOffset, endOffset, 1.0, context))
            }
            case "MISC" => {
              List(Entity(nodeId, -1, timestamp, "misc", word, beginOffset, endOffset, 1.0, context))
            }
            case _ => {
              List()
            }
          }
        })
    })
      .cache()

    namesRDD.saveToCassandra(Constants.keyspace, "entities_by_type", SomeColumns("nodeid", "nodeversion", "timestamp", "entitytype", "entity", "count", "beginoffset" as "beginOffset",
      "endoffset" as "endOffset", "probability" as "probability", "context" as "context"))

    namesRDD.saveToCassandra(Constants.keyspace, "entities_by_node", SomeColumns("nodeid", "nodeversion", "timestamp", "entitytype", "entity", "count", "beginoffset" as "beginOffset",
      "endoffset" as "endOffset", "probability" as "probability", "context" as "context"))
  }

  def receive = {
    case m:String => {
      println(s"Entities actor $m")
      val nodeAndPath = m.parseJson.convertTo[NodeAndPath]
      val nodeId = nodeAndPath.nodeId
      val path = nodeAndPath.path

      val sentencesRDD:RDD[Sentence] = sc.hadoopFile[LongWritable, Text, TextInputFormat](path)
        .filter(x => !x._2.toString().trim().isEmpty())
        .map(x => Sentence(nodeId, -1, x._1.get(), x._2.toString().trim(), DateTime.now()))
      getEntities(nodeId, sentencesRDD)

      val json = EntitiesAvailable(nodeId).toJson.toString
      println(s"sending entities available json $json")
      kafkaSink.send("org.alfresco.entities", json)

      sender ! StreamFSM.Processed
    }
    case x => {
      println(s"Entities actor $x")
      sender ! StreamFSM.Processed
    }
  }
}
