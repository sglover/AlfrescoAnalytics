package org.alfresco.analytics.client

import java.io._
import java.util.Random

import scala.collection.mutable
import scala.reflect.io

/**
  * Created by sglover on 03/01/2016.
  */
case class Files(path:String) {
  val f = new File(path)

  def getExtension(path:String):Option[String] = {
    val idx = path.lastIndexOf(".")
    if(idx > -1)
    {
      Some(path.substring(idx + 1))
    }
    else
    {
      None
    }
  }

  def getFilename(path:String):Option[String] = {
    val idx = path.lastIndexOf(File.separator)
    if(idx > -1)
    {
      Some(path.substring(idx + 1))
    }
    else
    {
      None
    }
  }

  def recursiveListFiles(f: File): List[FileData] = {
    f.listFiles().toList.flatMap(f1 => f1.isDirectory match {
        case true => recursiveListFiles(f1)
        case whatever => {
          val path = f1.getAbsolutePath()
          val mimeType = getExtension(path) match {
            case Some("pdf") => {
              "application/pdf"
            }
            case Some("txt") => {
              "plain/text"
            }
            case None => {
              "plain/text"
            }
            case _ => {
              ""
            }
          }
          val filename = getFilename(path)
          val size = f1.length()
          List(FileData(path, filename.getOrElse(""), mimeType, size))
        }
      })
  }

  val paths = recursiveListFiles(f)

  val r = new Random()

  def randomFile(): FileData = {
    paths(r.nextInt(paths.size))
  }
}

case class FileData(path:String, filename:String, mimeType:String, size:Long)
