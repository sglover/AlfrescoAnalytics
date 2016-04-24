package org.alfresco.analytics.actors

import java.io.FileOutputStream

import org.alfresco.transformation.api.{TransformResponse, TransformRequest, MimeType}
import org.alfresco.transformation.client.TransformationCallback
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException
import org.apache.commons.io.IOUtils
import akka.actor.Actor
import org.alfresco.analytics._
import com.sclasen.akka.kafka.StreamFSM
import spray.json._

/**
  * Created by sglover on 17/01/2016.
  */
class ContentUpdates(kafkaSink:KafkaSink, repoUsername:String, repoPassword:String) extends Actor with CMISOperations with DataSelection {
  //with ActorHelper {

//  lazy val cs = new FileContentSto
  lazy val t = Transform(repoUsername, repoPassword)

  override def preStart() = {
    println("")
    println("=== ContentUpdates is starting up ===")
    println(s"=== path=${context.self.path} ===")
    println(s"=== Hello ===")
    println("")
  }

  def convert(nodeId: String, mimeType: MimeType, nodePath: Option[String]): Unit = {
    try {
      val objectId = new ObjectIdImpl(nodeId)

      println(s"Content update $nodeId $mimeType $nodePath")

      try {}
      val temp = TempDirectory.createTemporaryFile("kafka")
      temp.deleteOnExit()
      println(s"Created temporary file $temp")
      val path = temp.getAbsolutePath()
      val os = new FileOutputStream(temp)

      println(s"Getting binary content for $nodeId $mimeType $path")

      val cs = getCMISSession(repoUsername, repoPassword).getContentStream(objectId)
      IOUtils.copy(cs.getStream(), os)

      println(s"Got binary content for $nodeId $mimeType $path")

      val callback = new TextTransformationCallback(nodeId, kafkaSink)
      t.sendTransform(path, mimeType, callback)
    } catch {
      case e:CmisObjectNotFoundException => println(s"Object with id $nodeId not found")
    }
  }

  def receive = {
    case m:String => {
      println(s"Content actor $m")
      m.parseJson.asJsObject().fields("type") match {
        case JsString("activity.org.alfresco.documentlibrary.file-updated") => {
          val event = Serializer.deserializeAsActivityEvent(m)
          getNodeId(event) match {
            case Some(nodeId) => {
              getMimeType(event) match {
                case Some(mimeType) => {
                  convert(nodeId, mimeType, None)
                }
              }
            }
          }
        }
        case _ =>
      }

      sender ! StreamFSM.Processed
    }
    case x => {
      println(s"xContent actor $x")
      sender ! StreamFSM.Processed
    }
  }
}

class TextTransformationCallback(nodeId:String, kafkaSink:KafkaSink) extends TransformationCallback {
  override def onError(r:TransformRequest, t:Throwable): Unit = {
    // TODO
    //    p failure t
  }

  override def transformCompleted(transformResponse: TransformResponse): Unit = {
    import MyJsonProtocol._

    val textPath = transformResponse.getTargets.get(0).getPath()
    println(s"textPath=$textPath")
    val json = TextTransformation(nodeId, textPath).toJson.toString
    println(s"sending text json $json")
    kafkaSink.send("org.alfresco.textcontent", json)
    //    p success textPath
  }
}
