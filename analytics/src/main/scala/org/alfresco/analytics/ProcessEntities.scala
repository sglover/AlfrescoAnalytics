package org.alfresco.analytics

import java.util.{Date, Properties}

import _root_.edu.stanford.nlp.ling.CoreAnnotations.{CharacterOffsetBeginAnnotation, CharacterOffsetEndAnnotation, NamedEntityTagAnnotation, PartOfSpeechAnnotation, TextAnnotation, TokensAnnotation}
import _root_.edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import com.datastax.spark.connector.{toNamedColumnRef, toRDDFunctions, _}
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapred.TextInputFormat
import org.apache.spark.Logging
import org.apache.spark.rdd.RDD
import org.joda.time.DateTime

import scala.collection.JavaConverters._

/**
  * Created by sglover on 17/12/2015.
  */
class ProcessEntities(nodeId:String, nodeVersion:Int, path:String) extends Serializable with Logging with Sparky with DB {

  def process(): Unit = {
    // closure
    val localPath = path
    val localNodeId = nodeId
    val localNodeVersion = nodeVersion

    // read the content of the file using Hadoop format
    val sentencesRDD:RDD[Sentence]  = sc.hadoopFile[LongWritable, Text, TextInputFormat](localPath)
      .filter(x => !x._2.toString().trim().isEmpty())
      .map(x => Sentence(localNodeId, localNodeVersion, x._1.get(), x._2.toString().trim(), DateTime.now()))

    val namesRDD = sentencesRDD.flatMap(sentence => {

      // create an empty Annotation just with the given text
      val document = new Annotation(sentence.line);

      // run all Annotators on this text
      MyCoreNLP.pipeline.annotate(document);

      // these are all the sentences in this document
      // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
      document
        .get(classOf[TokensAnnotation])
        .asScala.toList
        .flatMap(token => {
          // this is the text of the token
          val word = token.get(classOf[TextAnnotation]);
          // this is the POS tag of the token
          val pos = token.get(classOf[PartOfSpeechAnnotation])
          // this is the NER label of the token
          val ne: String = token.get(classOf[NamedEntityTagAnnotation])
          val beginOffset: Long = token.get(classOf[CharacterOffsetBeginAnnotation]) + sentence.offset
          val endOffset: Long = token.get(classOf[CharacterOffsetEndAnnotation]) + sentence.offset
          val context: String = ""
          val timestamp = DateTime.now().toDate()

          ne match {
            case "LOCATION" => {
              List(Entity(localNodeId, localNodeVersion, timestamp, "location", word, beginOffset, endOffset, 1.0, context))
            }
            case "PERSON" => {
              List(Entity(localNodeId, localNodeVersion, timestamp, "person", word, beginOffset, endOffset, 1.0, context))
            }
            case "ORGANIZATION" => {
              List(Entity(localNodeId, localNodeVersion, timestamp, "org", word, beginOffset, endOffset, 1.0, context))
            }
            case "MISC" => {
              List(Entity(localNodeId, localNodeVersion, timestamp, "misc", word, beginOffset, endOffset, 1.0, context))
            }
            case _ => {
              List()
            }
          }
        })
    })

    namesRDD.saveToCassandra(Constants.keyspace, "entities_by_type", SomeColumns("nodeid", "nodeversion", "timestamp", "entitytype", "entity", "count", "beginoffset" as "beginOffset",
      "endoffset" as "endOffset", "probability" as "probability", "context" as "context"))

    namesRDD.saveToCassandra(Constants.keyspace, "entities_by_node", SomeColumns("nodeid", "nodeversion", "timestamp", "entitytype", "entity", "count", "beginoffset" as "beginOffset",
      "endoffset" as "endOffset", "probability" as "probability", "context" as "context"))

    sc.stop()
    shutdown()
  }
}

object MyCoreNLP {
  val props = new Properties()
  props.put("annotators", "tokenize, ssplit, pos, lemma, ner")
  props.put("pos.maxlen", Int.box(300))
  props.put("useNGrams", Boolean.box(true))
  props.put("maxNGramLeng", Int.box(10))
  @transient lazy val pipeline = new StanfordCoreNLP(props)
}

object ProcessEntities {
  def apply(nodeId:String, nodeVersion:String, path:String):ProcessEntities = {
    new ProcessEntities(nodeId, nodeVersion.toInt, path)
  }

  def main(args: Array[String]): Unit = {
    val nodeId = args(0)
    val nodeVersion = args(1)
    val path = args(2)
    ProcessEntities(nodeId, nodeVersion, path).process()
  }
}