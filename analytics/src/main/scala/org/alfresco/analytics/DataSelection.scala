package org.alfresco.analytics

import org.alfresco.events.types.ActivityEvent
import org.alfresco.transformation.api.MimeType
import spray.json._
import org.alfresco.analytics.MyJsonProtocol._

/**
  * Created by sglover on 01/01/2016.
  */
trait DataSelection {

  val extractNodeIdFolder = """folder-details?nodeRef=workspace%3A%2F%2FSpacesStore%2F(.+)""".r
  val extractNodeId = """document-details\?nodeRef=workspace%3A%2F%2FSpacesStore%2F(.+)""".r

  def getMimeType(ae:ActivityEvent): Option[MimeType] = {
    ae.getMimeType() match {
      case null => None
      case something => {
        Some(MimeType.INSTANCES.getByMimetype(something))
      }
    }
  }

  def getNodeId(ae:ActivityEvent): Option[String] = {
    ae.getNodeId() match {
      case null => {
        ae.getActivityData() match {
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
}
