package org.alfresco.analytics

import java.io.File

import com.datastax.spark.connector.SomeColumns
import org.apache.spark.Logging
import com.datastax.spark.connector.{toNamedColumnRef, toRDDFunctions, _}

/**
  * Created by sglover on 18/12/2015.
  */
class Analytics extends Serializable with Logging with Sparky with DB {

  def process():Unit = {
    import sqlContext.implicits._

//    sc
//      .cassandraTable(keyspace, "activities_by_node")

//    yearAndMonths
//      .foreach {
//        x => {
//          val yearMonth:String = x._1
//          sqlContext
//            .sql(s"SELECT nodeid FROM activities$yearMonth WHERE tk_activitytype IN (9, 7, 2, 10, 5, 40, 17, 22, 39)")
//            .map {
//              r => (r.getAs[String]("nodeid"), 1)
//            }
//            .reduceByKey {
//              (v1, v2) => v1 + v2
//            }
//            .map {
//              x => NodeCount(yearMonth, x._1, x._2)
//            }
//            .saveToCassandra(keyspace, "popular_content", SomeColumns("yearmonth", "nodeid", "count"))
//        }
//      }
//
//
//    yearAndMonths
//      .foreach {
//        x => {
//          val yearMonth:String = x._1
//
//          sqlContext
//            .sql(s"SELECT siteid FROM activities$yearMonth WHERE tk_activitytype IN (9, 7, 2, 10, 5, 40, 17, 22, 39)")
//            .map {
//              r => (r.getAs[String]("siteid"), 1)
//            }
//            .reduceByKey {
//              (v1, v2) => v1 + v2
//            }
//            .map {
//              x => SiteCount(yearMonth, x._1, x._2)
//            }
//            .saveToCassandra(keyspace, "popular_sites", SomeColumns("yearmonth", "siteid", "count"))
//
//          sqlContext
//            .sql(s"SELECT username, nodeid FROM activities$yearMonth WHERE tk_activitytype IN (9, 7, 2, 10, 5, 40, 17, 22, 39)")
//            .map {
//              r => (yearMonth, r.getAs[String]("nodeid"), r.getAs[String]("username"))
//            }
//            .saveToCassandra(keyspace, "users_by_node", SomeColumns("yearmonth", "nodeid", "username"))
//
//          sqlContext
//            .sql(s"SELECT username, nodeid FROM activities$yearMonth WHERE tk_activitytype IN (9, 7, 2, 10, 5, 40, 17, 22, 39)")
//            .map {
//              r => (yearMonth, r.getAs[String]("username"),  r.getAs[String]("nodeid"))
//            }
//            .saveToCassandra(keyspace, "nodes_by_user", SomeColumns("yearmonth", "username", "nodeid"))
//        }
//      }
//
//    yearAndMonths
//      .foreach {
//        x => {
//          val yearMonth: String = x._1
//          //          sqlContext
//          //            .sql("SELECT siteid FROM activities" + yearMonth + " WHERE tk_activitytype IN (9, 7, 2, 10, 5, 40, 17, 22, 39)")
//
//          val usersToNodes = sc
//            .cassandraTable(keyspace, "nodes_by_user")
//            .select("username", "nodeid")
//            .where(s"yearmonth = '$yearMonth'")
//            //            .where("username = 'resplin'")
//            .map {
//            r => {
//              (r.getString("username"), r.getString("nodeid"))
//            }
//          }
//
//          val nodesToUsers = sc
//            .cassandraTable(keyspace, "users_by_node")
//            .select("nodeid", "username")
//            .where(s"yearmonth = '$yearMonth'")
//            .map {
//              r => {
//                (r.getString("nodeid"), r.getString("username"))
//              }
//            }
//
//          // node1 -> (user1, user2)
//          nodesToUsers
//            .join(nodesToUsers)
//            // different users touched the same node
//            .filter {
//            y => y._2._1 != y._2._2
//          }
//            // user2 -> (node1, user1)
//            .map {
//            y => (y._2._2, (y._1, y._2._1))
//          }
//            // join on user -> node2
//            .join(usersToNodes)
//            // (node1, node2)
//            .map {
//            x => (x._2._1._2, x._2._2, x._1, yearMonth)
//          }
//            .saveToCassandra(keyspace, "user_node_interest")
//        }
//      }

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

//    sc.stop()
//
//    shutdown()
  }
}

//object Analytics {
//  def apply():Analytics = {
//    new Analytics(path)
//  }
//
//  def main(args: Array[String]): Unit = {
//    val path = args(0)
//    Analytics(path).process()
//  }
//}
