package org.alfresco.analytics

import java.io.File

import com.github.tototoshi.csv._

import scala.collection.mutable

/**
  * Created by sglover on 16/12/2015.
  */
object Nodes {
  def main(args:Array[String]) = {
    val dir = new File(args(0))

    def getListOfFiles(dir:File):Seq[String] =
      dir.listFiles.filter(_.isFile).map(_.getAbsolutePath()).filter(_.contains("pentaho")).toList

    implicit object MyFormat extends DefaultCSVFormat {
      override val delimiter = '\t'
    }

    val paths = getListOfFiles(dir)
    val nodeids = mutable.Map.empty[String, Int]
    var count = 0

    paths
      .map { path =>
        path
      }
      .foreach { path => {
        println(path)

        val reader = CSVReader.open(new File(path))
        //        val it = reader.iterator
        reader
          //          .toStream
          //          .filter {
          //            x => {
          //              !x.startsWith("date_yyyymmdd")
          //            }
          //          }
          .foreach { fields => {
            count += 1
            val nodeid = fields.tail.tail.head
            if (nodeid != "guid") {
              nodeids.get(nodeid) match {
                case Some(id) => {
                  nodeids(nodeid) += 1
                }
                case None => {
                  nodeids += nodeid -> 1
                }
              }
            }
          }
        }
      }
      }

    println (count)
    nodeids
      .filter(_._2 > 1)
      .foreach( x => {
        println(x._1 + "=" + x._2)
      })
  }
}

