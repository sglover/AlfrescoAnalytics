package org.alfresco.analytics

import com.datastax.spark.connector._
import com.datastax.spark.connector.toNamedColumnRef
import com.datastax.spark.connector.toRDDFunctions
import org.apache.spark.Logging
import org.apache.spark.sql.types.{StringType, StructField, IntegerType, StructType}

/**
  * Created by sglover on 14/12/2015.
  */
//with Stream
class PopularSites(path:String) extends Serializable with Logging with Sparky with DB {
  def process():Unit = {
    logInfo("Popular1")

    import sqlContext.implicits._

//    val customSchema = StructType(Array(StructField("guid", StringType), StructField("tk_activitytype", IntegerType)))

    sc
      .textFile(path)
      .map { str =>
        val p = str.split("\t")
        EventData(p(0), p(2), p(3), p(4), p(5))
      }
      .toDF()
      .registerTempTable("activities")

//    sqlContext
    //      .read
    //      .format("com.databricks.spark.csv")
    //      .option("header", "true") // Use first line of all files as header
    //      .option("inferSchema", "true") // Automatically infer data types
    //      .load(path)
    //      .registerTempTable("activities")

    //.show(2)
    //      .groupBy()
    //    .select("guid", "tk_activitytype")
    //    .filter($"tk_activitytype")

    val activities = sqlContext
      .sql("SELECT siteId AS id, tk_activitytype AS event_type FROM activities WHERE tk_activitytype IN (40, 17, 39)")
      .map { r => (r.getAs[String]("id"), 1) }
      .reduceByKey( (v1, v2) => v1 + v2 )
      .map( x => Doc(x._1, x._2))
      .saveToCassandra(Constants.keyspace, "popular", SomeColumns("id", "count"))

    sc.stop()
//    db.shutdown()
    shutdown()
  }
}

object PopularSites {
  def apply(path:String):PopularSites = {
    new PopularSites(path)
  }

  def main(args: Array[String]): Unit = {
    val path = args(0)
    PopularSites(path).process()
  }
}

case class Doc(id:String, count:Int)
case class EventData(date_yyyymmdd: String, guid: String, username: String, tk_activitytype: String, siteId: String)