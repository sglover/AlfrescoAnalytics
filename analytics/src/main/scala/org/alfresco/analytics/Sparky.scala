package org.alfresco.analytics

import org.apache.spark.SparkConf
import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by sglover on 14/12/2015.
  */
trait Sparky {
  val batchSize = 10

  @transient val sparkConf = new SparkConf(true)
    .setAppName("Analytics")
    .setMaster("local[4]")
    .set("spark.default.parallelism", "1")
    .set("spark.cassandra.connection.host", "127.0.0.1")
    .set("textinputformat.record.delimiter", ". ")
  @transient lazy val sc = new SparkContext(sparkConf)
  sc.setLogLevel("INFO")
  @transient lazy val streamingContext = new StreamingContext(sc, Seconds(batchSize))
  @transient val d = TempDirectory.createTemporaryDirectory("Analytics", "Spark")
  sys.addShutdownHook(TempDirectory.removeTemporaryDirectory(d))
  streamingContext.checkpoint(d.getAbsolutePath)
  @transient lazy val cassandra = CassandraConnector(sparkConf)
//  @transient lazy val sqlContext = new SQLContext(sc)
  lazy val sqlContext = new SQLContext(sc)
}
