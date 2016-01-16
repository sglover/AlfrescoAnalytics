package org.alfresco.analytics

import org.alfresco.transformation.api.MimeType
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row}
import spray.json._
import com.datastax.spark.connector._
import org.alfresco.analytics.MyJsonProtocol._

/**
  * Created by sglover on 10/01/2016.
  */
trait SparkDataSelection extends DataSelection {
  this: Sparky =>

  def hashIt(str:String): Int = {
    str.hashCode.toInt
  }

  def getPath(r:Row): Option[String] = {
    val idx = r.fieldIndex("paths")
    val nodePaths = r.getList[String](idx)
    nodePaths.size() match {
      case 0 => None
      case _ => Some(nodePaths.get(0))
    }
  }

  def getMimeType(r:Row): Option[MimeType] = {
    r.getAs[String]("mimeType") match {
      case null => None
      case something => {
        Some(MimeType.INSTANCES.getByMimetype(something))
      }
    }
  }

  def getNodeId(r:Row): Option[String] = {
    r.getAs[String]("nodeId") match {
      case null => {
        r.getAs[String]("activityData") match {
          case null => {
            None
          }
          case json => {
            json.parseJson.convertTo[ActivityData].page match {
              case extractNodeId(nodeId) => Some(nodeId)
              case extractNodeIdFolder(nodeId) => Some(nodeId)
              case _ => None
            }
          }
        }
      }
      case nodeId => {
        Some(nodeId)
      }
    }
  }

  def updates(tableName:String): DataFrame = {
    sqlContext
      .table(tableName)
      .select("username", "nodeId", "siteId", "type", "paths", "mimeType")
      .where("type IN ('activity.org.alfresco.documentlibrary.file-updated')")
      .cache()
  }

  def interestingEvents(tableName:String): DataFrame = {
    sqlContext
      .table(tableName)
      .select("username", "nodeId", "siteId", "type")
      .where("type IN ('activity.org.alfresco.documentlibrary.file-previewed', 'activity.org.alfresco.documentlibrary.file-liked', 'activity.org.alfresco.comments.comment-created')")
      //.sql(s"SELECT username, nodeId, siteId FROM $tableName WHERE type IN ('activity.org.alfresco.documentlibrary.file-previewed', 'activity.org.alfresco.documentlibrary.file-liked', 'activity.org.alfresco.comments.comment-created');")
      .cache()
  }

  def userRatings(userId:String): RDD[Rating] = {
    sc
      .cassandraTable(Constants.keyspace, "ratings_by_user")
      .select("userid", "nodeid", "rating", "timestamp")
      .where(s"userid = '$userId'")
      .map {
        r => {
          Rating(hashIt(r.getString("userid")), hashIt(r.getString("nodeid")), r.getInt("rating"))
        }
      }
  }

  def userRatingsForYearMonth(yearMonth:String): RDD[(Long, Rating)] = {
    sc
      .cassandraTable(Constants.keyspace, "ratings_by_user")
      .select("userid", "nodeid", "rating", "timestamp")
      .where(s"yearMonth = '$yearMonth' AND type IN ('activity.org.alfresco.documentlibrary.file-liked', 'activity.org.alfresco.comments.comment-created')")
      .map {
        r => {
          (r.getLong("timestamp") % 10, Rating(hashIt(r.getString("userid")), hashIt(r.getString("nodeid")), r.getInt("rating")))
        }
      }
  }

  def usersByNode(yearMonth:String): RDD[(String, String)] = {
    sc
      .cassandraTable(Constants.keyspace, "users_by_node")
      .select("nodeid", "username")
      .where(s"yearmonth = '$yearMonth'")
      .map {
        r => {
          (r.getString("nodeid"), r.getString("username"))
        }
      }
  }

  def nodesByUser(yearMonth:String): RDD[(String, String)] = {
    sc
      .cassandraTable(Constants.keyspace, "nodes_by_user")
      .select("username", "nodeid")
      .where(s"yearmonth = '$yearMonth'")
      //            .where("username = 'resplin'")
      .map {
      r => (r.getString("username"), r.getString("nodeid"))
    }
  }

  def popularSites(yearMonth:String): RDD[(SiteKey, Int)] = {
    sc
      .cassandraTable(Constants.keyspace, "popular_sites")
      .select("yearmonth", "count", "siteid")
      .where(s"yearmonth = '$yearMonth'")
      .map {
        r => (SiteKey(r.getString("yearmonth"), r.getString("siteid")), r.getInt("count"))
      }
  }
}
