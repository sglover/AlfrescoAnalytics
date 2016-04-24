package org.alfresco.analytics

import java.util.UUID
import com.datastax.spark.connector._
import akka.actor.{ActorSystem, Props}
import com.datastax.spark.connector.SomeColumns
import com.sclasen.akka.kafka.{AkkaConsumer, AkkaConsumerProps}
import com.typesafe.config.ConfigFactory
import kafka.serializer.StringDecoder
import org.alfresco.analytics.MyJsonProtocol._
import org.alfresco.analytics.actors.{ContentUpdates, Entities}
import org.alfresco.events.types.ActivityEvent
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{LongType, StringType, DoubleType, StructField, StructType}
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.kafka.KafkaUtils
import spray.json._

/**
  * Created by sglover on 17/12/2015.
  */
class Kafka(repoUsername:String, repoPassword:String) extends DB with Sparky with Serializable with Calculations with SparkDataSelection with Recommendations {
  this: Sparky =>

//  @transient val log = LogManager.getLogger(classOf[Kafka])

  val kafkaParams = Map("metadata.broker.list" -> "localhost:9092")

  val kafkaProducerParams = Map("bootstrap.servers" -> "localhost:9092",
    "key.serializer" -> "org.apache.kafka.common.serialization.StringSerializer",
    "value.serializer" -> "org.apache.kafka.common.serialization.StringSerializer"
  )
  @transient val kafkaSink = KafkaSink(kafkaProducerParams)
  val kafkaOut = sc.broadcast(kafkaSink)

  // listen for repo activities events
  @transient val activities = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
    streamingContext, kafkaParams, Set("alfresco.repo.activities"))
    .cache()
  activities.checkpoint(Seconds(batchSize*5))

  // listen for control events
  @transient val control = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
    streamingContext, kafkaParams, Set("analytics.control"))
    .cache()
  control.checkpoint(Seconds(batchSize*5))

  @transient val config = ConfigFactory.load()

  @transient val actorSystem = ActorSystem("ContentActorSystem", config.getConfig("ContentActorSystem").withFallback(config))
  @transient val contentUpdatesActor = actorSystem.actorOf(Props(new ContentUpdates(kafkaSink,
    repoUsername, repoPassword)), "contentUpdates")

  // listen for repo activities events and inform the content update actor (which will transform to text)
  @transient val activitiesConsumerProps = AkkaConsumerProps.forSystem(
    system = actorSystem,
    zkConnect = "localhost:2181",
    topic = "alfresco.repo.activities",
    group = "group2000",
    streams = 4, //one per partition
    keyDecoder = new StringDecoder(),
    msgDecoder = new StringDecoder(),
    receiver = contentUpdatesActor
  )
  @transient val activitiesConsumer = new AkkaConsumer(activitiesConsumerProps)
  activitiesConsumer.start()  //returns a Future[Unit] that completes when the connector is started

  // listen for transformed text content events and inform the entities actor
  @transient val entitiesActor = actorSystem.actorOf(Props(new Entities(sc, kafkaSink, repoUsername, repoPassword)), "entitiesActor")
  @transient val textContentConsumerProps = AkkaConsumerProps.forSystem(
    system = actorSystem,
    zkConnect = "localhost:2181",
    topic = "org.alfresco.textcontent",
    group = "group2001",
    streams = 4, //one per partition
    keyDecoder = new StringDecoder(),
    msgDecoder = new StringDecoder(),
    receiver = entitiesActor
  )
  @transient val textContentConsumer = new AkkaConsumer(textContentConsumerProps)
  textContentConsumer.start()  //returns a Future[Unit] that completes when the connector is started

  def guid(): String = {
    UUID.randomUUID().toString().replaceAll("-", "")
  }

  // deal with control messages
  control.foreachRDD { (rdd, time) => {
    if (!rdd.toLocalIterator.isEmpty) {
      val yearMonth = Constants.getYearMonth(time.milliseconds)

      sqlContext
        .read
        .json(rdd.map {
          case (_, msg) => {
            println(s"control msg=$msg")
            msg
          }
        })
        .map {
          x => {
            x.getAs[String]("msg") match {
              case "buildRecommender" => {
                println(s"Building ratings model for $yearMonth")
                buildRatingsModelForYearMonth(yearMonth)
              }
            }
          }
        }
    }
    }
  }

  // calculate "interesting nodes" and node interest by site
  activities.foreachRDD { (rdd, time) => {
    if (!rdd.toLocalIterator.isEmpty) {
      val yearMonth = Constants.getYearMonth(time.milliseconds)
      println("yearMonth " + yearMonth)

      // Register as table
      val rdd0 = sqlContext
        .read
        .json(rdd.map {
          case (_, msg) => {
            println(s"msg=$msg")
            msg
          }
        })
        .cache()
      println("rdd0.first5 = " + firstN(rdd0, 5))

      val rdd1 = rdd0
        .filter( """`@class` = "org.alfresco.events.types.ActivityEvent"""")
        .map {
          r => {
            println(s"r=$r")

            val nodeId = getNodeId(r)
            val mimeType = getMimeType(r).map(x => x.getMimetype).getOrElse("")

            val rating = r.getAs[String]("type") match {
              case "activity.org.alfresco.documentlibrary.file-liked" => 10.0
              case "activity.org.alfresco.comments.comment-created" => 10.0
              case _ => 0.0
            }

            Row(r.getAs[String]("txnId"), nodeId.getOrElse(""), r.getAs[String]("siteId"), r.getAs[String]("username"),
              r.getAs[String]("type"), r.getAs[String]("nodeType"), mimeType, rating, r.getAs[Long]("timestamp"),
              yearMonth)
          }
        }
        .filter {
          row => row.get(1) != ""
        }
        .cache()

      rdd1
        .saveToCassandra(Constants.keyspace, s"ratings_by_user", SomeColumns("yearmonth", "type", "userid", "nodeid", "rating"))

      val schema =
        StructType(
          Seq(StructField("txnId", StringType, false),
            StructField("nodeId", StringType, false),
            StructField("siteId", StringType, false),
            StructField("username", StringType, false),
            StructField("type", StringType, false),
            StructField("nodeType", StringType, false),
            StructField("mimeType", StringType, false),
            StructField("rating", DoubleType, false),
            StructField("timestamp", LongType, false)))

      val guid1 = guid()
      val tableName1 = s"activities$guid1"
      sqlContext
        .createDataFrame(rdd1, schema)
        .registerTempTable(tableName1)
      println(s"Registered as temp table $tableName1")
      println("tempTable1.first = " + firstN(sqlContext.table(tableName1), 5))

      // Node interest

      val guid2 = guid()
      val tableName2 = s"activities$guid2"
      sqlContext
        .createDataFrame(rdd1, schema)
        .registerTempTable(tableName2)
      println(s"Registered as temp table $tableName2")
      println("tempTable2.first = " + firstN(sqlContext.table(tableName2), 5))

      val existingPopularSitesRDD = popularSites(yearMonth).cache()
      val deltaRDD = interestingEvents(tableName2)

      println("delta first = " + firstN(deltaRDD, 5))

      updateSiteCounts(deltaRDD, yearMonth, existingPopularSitesRDD)

      val eventsByUserAndNodeRDD = deltaRDD
        .map {
          r => {
            val nodeId = r.getAs[String]("nodeId")
            val username = r.getAs[String]("username")
            (yearMonth, nodeId, username)
          }
        }
        .cache()

      println("eventsByUserAndNodeRDD.first = " + firstN(eventsByUserAndNodeRDD, 5))

      eventsByUserAndNodeRDD.saveToCassandra(Constants.keyspace, "users_by_node", SomeColumns("yearmonth", "nodeid", "username"))
      eventsByUserAndNodeRDD.saveToCassandra(Constants.keyspace, "nodes_by_user", SomeColumns("yearmonth", "username", "nodeid"))

      val currentNodesByUserRDD = nodesByUser(yearMonth).cache()
      println("currentNodesByUserRDD.first = " + firstN(currentNodesByUserRDD, 5))

      val currentUsersByNodeRDD = usersByNode(yearMonth).cache()
      println("currentUsersByNodeRDD.first = " + firstN(currentUsersByNodeRDD, 5))

      val countsByNodeInterestRDD = countsByNodeInterest(currentUsersByNodeRDD)
      println("countsByNodeInterestRDD.first = " + firstN(countsByNodeInterestRDD, 5))

      val nodesToUsersDeltaRDD = deltaRDD
        .map {
          r => (r.getAs[String]("nodeId"), r.getAs[String]("username"))
        }

      println("nodesToUsersDeltaRDD.first = " + firstN(nodesToUsersDeltaRDD, 5))

      val nodeInterestRDD = nodeInterest(nodesToUsersDeltaRDD, currentUsersByNodeRDD, currentNodesByUserRDD, countsByNodeInterestRDD)
        // node pairs, first node is from the node delta
        .map {
        x => x._2
      }

      val nodes = nodeInterestRDD.take(10)
      val aEventStr = InterestingNodes(nodes).toJson.toString()
      kafkaOut.value.send("watched.analytics", aEventStr)

      // cleanup
      //deltaRDD.unpersist()
    }
  }
  }

  // calculate "popular content"
  activities
    .map { case (_, v) => Serializer.deserializeAsEvent(v) }
    .map { event => {
      println("event " + event)

      val eventStr = Serializer.serializeAsEvent(event)
      kafkaOut.value.send("watched.activities", eventStr)

      val yearMonth = Constants.getYearMonth(event)

      println("yearMonth " + yearMonth)

      event match {
        case ae: ActivityEvent => {
          val nodeId = ae.getNodeId() match {
            case null => {
              ae.getActivityData() match {
                case null => {
                  None
                }
                case json => {
                  json.parseJson.convertTo[ActivityData].page match {
                    case extractNodeId(nodeId) => Some(nodeId)
                    case _ => None
                  }
                }
              }
            }
            case _ => {
              Some(ae.getNodeId())
            }
          }

          (nodeId.getOrElse(""), (1, yearMonth))
        }
        case something => {
          ("", (0, yearMonth))
        }
      }
    }
    }
    .filter(x => x._1 != "" && x._1 != Nil)
    .reduceByKeyAndWindow(
      (x, y) => (x._1 + y._1, x._2),
      (x, y) => (x._1 - y._1, x._2),
      Seconds(batchSize * 2),
      Seconds(batchSize)
    )
    .filter {
      case (_, (count, _)) => count > 0
    }
    .foreachRDD((rdd, time) => {
      if (rdd.toLocalIterator.nonEmpty) {
        rdd
          .map(x => WindowedNodeCount(x._2._2, time.milliseconds, x._1, x._2._1))
          .saveToCassandra(Constants.keyspace, s"popular_content", SomeColumns("yearmonth", "timestamp", "nodeid", "count"))

        val popular = rdd
          .map(x => WindowedNodeCount(x._2._2, time.milliseconds, x._1, x._2._1))
          .aggregate(List[WindowedNodeCount]())(
            (l, x) => List(x) ++ l,
            (l1, l2) => l1 ++ l2
          )

        val aEventStr = PopularEvent(popular).toJson.toString()
        kafkaOut.value.send("watched.analytics", aEventStr)
      }
    })

  streamingContext.start()
  streamingContext.awaitTermination()

  activitiesConsumer.commit() //returns a Future[Unit] that completes when all in-flight messages are processed and offsets are committed.
  activitiesConsumer.stop()   //returns a Future[Unit] that completes when the connector is stopped.
}

object Kafka {
  def apply(repoUsername:String, repoPassword:String): Kafka = {
    new Kafka(repoUsername, repoPassword)
  }
//  private def shutdown(): Unit = if (!isTerminated) {
//    import akka.pattern.ask
//    if (_isTerminated.compareAndSet(false, true)) {
//      log.info("Node {} shutting down", selfAddress)
//      cluster leave selfAddress
//      kafka.shutdown()
//      ssc.stop(stopSparkContext = true, stopGracefully = true)
//
//      (guardian ? GracefulShutdown).mapTo[Future[Boolean]]
//        .onComplete { _ =>
//          system.shutdown()
//          system.awaitTermination(timeout.duration)
//        }
//    }
//  }

  def main(args: Array[String]): Unit = {
    Kafka("admin", "admin")
  }
}
