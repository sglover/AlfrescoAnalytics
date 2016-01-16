package org.alfresco.analytics

import com.datastax.spark.connector.{toNamedColumnRef, toRDDFunctions, _}
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.pipeline.Annotation
import org.alfresco.analytics.Constants._
import org.apache.spark.rdd._
import org.apache.spark.sql.DataFrame
import org.joda.time.DateTime

import scala.collection.JavaConverters._

/**
  * Created by sglover on 23/12/2015.
  */
trait Calculations {
  val threshold = 2

  //      val rdd = sc
  //        .textFile("")
  //        .map(x => x.split(", "))
  //        .map(row => row.map(x => x.toDouble))
  //      val schema = StructType(Array(StructField("1", DoubleType,true)))
  //      val dfpen = sqlContext.createDataFrame(rdd.map(Row.fromSeq(_)), schema)
  //      val va = new VectorAssembler().setOutputCol("features")
  //      va.setInputCols(dfpen.columns.diff(Array("label")))
  //      val penrdd = va.transform(dfpen).select("features", "label")
  //        .map { row => (row(0).asInstanceOf[Vector], row(1).asInstanceOf[Double]) }
  //        .map { row => row._1 }
  //        .cache()

  /**
    * Count the number of node views by pairs of users that have viewed the node
    *
    * @param usersToNodes
    * @return pairs of (node, count) for 2 users that have viewed the node
    */
  def countsByNodeInterest(usersToNodes:RDD[(userId, nodeId)]): RDD[(nodeId, count)] = {
    // node1 -> (user1, user2)
    usersToNodes
      .join(usersToNodes)
      .filter {
        // different users touched the same node
        y => y._2._1 != y._2._2
      }
      .mapValues(_ => 1)
      .reduceByKey(_ + _)
      .cache()
  }

  def firstN(df:DataFrame, n:Int): String = {
    df.count() match {
      case 0l => "<empty>"
      case l:Long => df.take(n)
        .map { r => {
          println(s"firstN.r=$r")
          r.toString()
        }
        }
        .reduce { (a, b) =>  a + ", " + b }
    }
  }

  def firstN(rdd:RDD[_], n:Int): String = {
    rdd.count() match {
      case 0l => "<empty>"
      case l: Long => rdd.take(n)
        .map {
          r => r.toString()
        }
        .reduce { (a, b) =>  a + ", " + b
        }
    }
  }

  // TODO reduceByKey vs mapValues?
  def updateSiteCounts(delta:DataFrame, yearMonth:String, existing:RDD[(SiteKey, count)]): Unit = {
    val x = delta
    .map {
      r => (SiteKey(yearMonth, r.getAs[String]("siteId")), 1)
    }
    .reduceByKey {
      (v1, v2) => v1 + v2
    }
    .cache()

    println("updateSiteCounts.x.first5 = " + firstN(x, 5))

    val y = x.leftOuterJoin(existing)
    .cache()

    println("updateSiteCounts.y.first5 = " + firstN(x, 5))

    y.mapValues {
      x => x._1 + x._2.getOrElse(0)
    }
    .map {
      x => SiteCount(x._1.yearMonth, x._1.siteId, x._2)
    }
    .saveToCassandra(Constants.keyspace, "popular_sites", SomeColumns("yearmonth" as "yearMonth", "siteid" as "siteId", "count" as "count"))
  }

  def nodeInterest(nodesToUsersDelta:RDD[(nodeId, userId)], nodesToUsers:RDD[(nodeId, userId)],
                   usersToNodes:RDD[(userId, nodeId)], countsByNodeInterestRDD:RDD[(nodeId, count)]): RDD[(String, String)] = {
    // node1 -> (user1, user2) for nodes in nodesToUsersDelta
    nodesToUsersDelta
      .join(nodesToUsers)
      // different users touched the same node
      .filter {
        x => x._2._1 != x._2._2
      }
      // user2 -> (node1, user1)
      .map {
        x => (x._2._2, (x._1, x._2._1))
      }
      // join on user2 -> node2
      // user2 -> ((node1, user1), node2)
      .join(usersToNodes)
      // filter out common nodes i.e. node1 != node2
      .filter {
        x => x._2._1._1 != x._2._2
      }
      // (node2, node1)
      .map {
        x => (x._2._2, x._2._1._1)
      }
      // join with counts for node2 interest
      .join(countsByNodeInterestRDD)
      // only interested in those matches above the threshold
      .filter {
        x => x._2._2 > threshold
      }
      // (node1, node2)
      .map {
        x => (x._2._1, x._1)
      }
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
}
