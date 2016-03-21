package org.alfresco.analytics

import java.io.File

import com.datastax.spark.connector.{toNamedColumnRef, toRDDFunctions, _}
import org.apache.spark._
import org.apache.spark.rdd.RDD._

/**
  * Created by sglover on 14/12/2015.
  */
class PopularContentProcessor(path:String) extends Serializable with Logging with Sparky with DB {
  val dir = new File(path)
  val paths = getListOfFiles(dir)

  def getListOfFiles(dir:File):Seq[String] =
    dir.listFiles.filter(_.isFile).map(_.getAbsolutePath()).filter(_.contains("pentaho")).toList

  def process():Unit = {
    import sqlContext.implicits._

    val yearAndMonths = paths
      .map { path =>
        println(path)
        path
      }
      .map { path => {
          (sc
            .textFile(path)
            .take(2)(1)
            .split("\t")(0).substring(0, 6),
            path)
        }
      }

    yearAndMonths
    .foreach {
      x => {
        val yearMonth = x._1
        val path = x._2
        sc
          .textFile(path)
          .map { str =>
            val p = str.split("\t")
            ContentEventData(p(0), yearMonth, p(1), p(2), p(3), p(4), p(5), p(6), p(7), p(8))
          }
          .filter {
            c => {
              c.date_yyyymmdd != "date_yyyymmdd"
            }
          }
          .toDF()
          .registerTempTable("activities" + yearMonth)
      }
    }

//    paths
//      .map { path =>
//        println(path)
//        path
//      }
//      .map { path =>
//        sc
//          .textFile(path)
//          .take(2)(1)
//          .map { str =>
//            val p = str.split("\t")
//            p(0).substring(0, 6)
//          }
//          .filter {
//            c => {
//              c.date_yyyymmdd != "date_yyyymmdd"
//            }
//          }
//          .toDF()
//      }

//    paths
//      .map { path =>
//        println(path)
//        path
//      }
//      .map { path =>
//        sc
//          .textFile(path)
//          .map { str =>
//            val p = str.split("\t")
//            val yearAndMonth = p(0).substring(0, 6)
//            ContentEventData(p(0), yearAndMonth, p(1), p(2), p(3), p(4), p(5), p(6), p(7), p(8))
//          }
//          .filter {
//            c => {
//              c.date_yyyymmdd != "date_yyyymmdd"
//            }
//          }
//          .toDF()
//      }
//      .reduce {
//        (x, y) => x.unionAll(y)
//      }
//      .registerTempTable("activities")

    yearAndMonths
      .foreach {
        x => {
          val yearMonth:String = x._1
          sqlContext
            .sql(s"SELECT nodeid FROM activities$yearMonth WHERE tk_activitytype IN (9, 7, 2, 10, 5, 40, 17, 22, 39)")
            .map {
              r => (r.getAs[String]("nodeid"), 1)
            }
            .reduceByKey {
              (v1, v2) => v1 + v2
            }
            .map {
              x => NodeCount(yearMonth, x._1, x._2)
            }
            .saveToCassandra(Constants.keyspace, "popular_content", SomeColumns("yearmonth", "nodeid", "count"))
        }
      }

    //    sqlContext
    //      .sql("SELECT month, nodeid, count(username) AS count FROM activities GROUP by month, nodeid ORDER BY count desc")
    //      .map {
    //        r => (r.getAs[String]("month"), r.getAs[String]("nodeid"), r.getAs[Int]("count"))
    //      }
    //      .saveToCassandra(keyspace, "popular_content")

    //    sqlContext
    //      .sql("SELECT username, nodeid, tk_activitytype AS event_type FROM activities WHERE tk_activitytype IN (9, 7, 2, 10, 5, 40, 17, 22, 39)")
    //      .map {
    //        r => (r.getAs[String]("nodeid"),
    //          r.getAs[String]("username"))
    //      }
    //      .aggregateByKey(0)(
    //        (acc, u) => acc + 1,
    //        (acc1, acc2) => acc1 + acc2
    //      )
    //      .saveToCassandra(keyspace, "counts_by_node", SomeColumns("nodeid", "count"))

    yearAndMonths
      .foreach {
        x => {
          val yearMonth:String = x._1

          sqlContext
            .sql(s"SELECT siteid FROM activities$yearMonth WHERE tk_activitytype IN (9, 7, 2, 10, 5, 40, 17, 22, 39)")
            .map {
              r => (SiteKey(yearMonth, r.getAs[String]("siteid")), 1)
            }
            .reduceByKey {
              (v1, v2) => v1 + v2
            }
            .map {
              x => SiteCount(x._1.yearMonth, x._1.siteId, x._2)
            }
            .saveToCassandra(Constants.keyspace, "popular_sites", SomeColumns("yearmonth", "siteid", "count"))

          sqlContext
            .sql(s"SELECT username, nodeid FROM activities$yearMonth WHERE tk_activitytype IN (9, 7, 2, 10, 5, 40, 17, 22, 39)")
            .map {
              r => (yearMonth, r.getAs[String]("nodeid"), r.getAs[String]("username"))
            }
            .saveToCassandra(Constants.keyspace, "users_by_node", SomeColumns("yearmonth", "nodeid", "username"))

          sqlContext
            .sql(s"SELECT username, nodeid FROM activities$yearMonth WHERE tk_activitytype IN (9, 7, 2, 10, 5, 40, 17, 22, 39)")
            .map {
              r => (yearMonth, r.getAs[String]("username"),  r.getAs[String]("nodeid"))
            }
            .saveToCassandra(Constants.keyspace, "nodes_by_user", SomeColumns("yearmonth", "username", "nodeid"))
        }
      }

    yearAndMonths
      .foreach {
        x => {
          val yearMonth: String = x._1
//          sqlContext
//            .sql("SELECT siteid FROM activities" + yearMonth + " WHERE tk_activitytype IN (9, 7, 2, 10, 5, 40, 17, 22, 39)")

          val usersToNodes = sc
            .cassandraTable(Constants.keyspace, "nodes_by_user")
            .select("username", "nodeid")
            .where(s"yearmonth = '$yearMonth'")
//            .where("username = 'resplin'")
            .map {
              r => {
                (r.getString("username"), r.getString("nodeid"))
              }
            }

          val nodesToUsers = sc
            .cassandraTable(Constants.keyspace, "users_by_node")
            .select("nodeid", "username")
            .where(s"yearmonth = '$yearMonth'")
            .map {
              r => {
                (r.getString("nodeid"), r.getString("username"))
              }
            }

          // node1 -> (user1, user2)
          nodesToUsers
            .join(nodesToUsers)
            // different users touched the same node
            .filter {
              y => y._2._1 != y._2._2
            }
            // user2 -> (node1, user1)
            .map {
              y => (y._2._2, (y._1, y._2._1))
            }
            // join on user -> node2
            .join(usersToNodes)
            // (node1, node2)
            .map {
              x => (x._2._1._2, x._2._2, x._1, yearMonth)
            }
            .saveToCassandra(Constants.keyspace, "user_node_interest")
        }
      }

//    sc
//      .cassandraTable(keyspace, "nodes_by_user")
//      .select("username")
////      .where("username IN ('resplin', 'ckitchener')")
//      .foreach {
//        x => {
//
//        }
//      }
//
//    // node1 -> (user1, user2)
//    nodesToUsers
//      .join(nodesToUsers)
//      // different users touched the same node
//      .filter {
//        y => y._2._1 != y._2._2
//      }
//      // user2 -> (node1, user1)
//      .map {
//        y => (y._2._2, (y._1, y._2._1))
//      }
//      // join on user -> node2
//      .join(usersToNodes)
//      // (node1, node2)
//      .map {
//        x => (x._2._1._2, x._2._2)
//      }
//      .saveToCassandra(keyspace, "user_node_interest")

    sc.stop()

    shutdown()
  }
}

object PopularContentProcessor {
//  def apply(path:String):PopularContent = {
//    new PopularContent(path)
//  }
//
//  def main(args: Array[String]): Unit = {
//    val path = args(0)
//    PopularContent(path).process()
//  }
}