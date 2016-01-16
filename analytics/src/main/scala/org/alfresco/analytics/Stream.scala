package org.alfresco.analytics

import com.datastax.spark.connector.SomeColumns
import kafka.serializer.StringDecoder
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka._
import org.apache.spark.sql.cassandra.CassandraSQLContext
import com.datastax.spark.connector._
import com.datastax.spark.connector.toNamedColumnRef
import com.datastax.spark.connector.toRDDFunctions

/**
  * Created by sglover on 14/12/2015.
  */
trait Stream {
  this: Sparky =>

//  val ssc = new StreamingContext(sparkConf, Seconds(2))

//  val topics: String
//  val brokers: String

  // Create direct kafka stream with brokers and topics
//  val topicsSet = topics.split(",").toSet
//  val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
//  val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
//    ssc, kafkaParams, topicsSet)

//  val x = sc.accumulator(0)

  val sqlContext = new SQLContext(sc)

  // Start the computation
//  ssc.start()
//  ssc.awaitTermination()
}
