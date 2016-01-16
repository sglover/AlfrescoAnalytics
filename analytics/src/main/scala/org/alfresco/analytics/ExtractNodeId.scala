package org.alfresco.analytics

import scala.util.matching.Regex

/**
  * Created by sglover on 04/01/2016.
  */
case class ExtractNodeId() {
  val extractNodeId = """document-details\?nodeRef=workspace%3A%2F%2FSpacesStore%2F(.+)""".r
  val s = "document-details?nodeRef=workspace%3A%2F%2FSpacesStore%2F4ebe07ed-9a2c-4306-a7d5-3e0288cdb0a9"
  println(s match {
    case extractNodeId(nodeId) => Some(nodeId)
    case _ => None
  })
}

object ExtractNodeId {
  def main(args:Array[String]): Unit = {
    ExtractNodeId()
  }
}
