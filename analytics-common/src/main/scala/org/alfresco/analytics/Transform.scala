package org.alfresco.analytics

/**
  * Created by sglover on 23/12/2015.
  */

import java.io.FileOutputStream

import org.alfresco.transformation.api.MimeType.MimeTypesMap
import org.alfresco.transformation.api.options.TransformationOptions
import org.alfresco.transformation.api.{ContentReference, MimeType, TransformRequest, TransformResponse}
import org.alfresco.transformation.client.{TransformationCallback, TransformationClient}
import org.alfresco.transformation.config.ClientConfig
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException
import org.apache.commons.io.IOUtils

import scala.collection.JavaConversions._

object Transform {
  def apply(repoUsername:String, repoPassword:String) = {
    new Transform(repoUsername, repoPassword)
  }
}

/**
  * Created by sglover on 11/12/2015.
  */
class Transform(repoUsername:String, repoPassword:String) extends CMISOperations {
  val routers = List("localhost:2551")
  val config:ClientConfig = new ClientConfig("localhost", 2192, routers)
  val client:TransformationClient = new TransformationClient(config)

  def closeClient() =
  {
    if(client != null)
    {
      client.disconnect()
    }
  }

  def convert(nodeId: String, mimeType: MimeType, callback:TransformationCallback): Unit = {
    try {
      val objectId = new ObjectIdImpl(nodeId)

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

      sendTransform(path, mimeType, callback)
    } catch {
      case e:CmisObjectNotFoundException => println(s"Object with id $nodeId not found")
    }
  }

  def sendTransform(path:String, mimeType:MimeType, callback:TransformationCallback) =
  {
    val ref = new ContentReference(path, mimeType)
    val options:TransformationOptions = new TransformationOptions()
    options.setMimetype(MimeType.TEXT)
    client.transform(new TransformRequest(ref, options), callback)
  }
}

object TransformTest {
  def main(args:Array[String]) = {
    val path = args(0)
    val t = new Transform("admin", "admin")
//    t.sendTransform(path, MimeType.INSTANCES.)
    val c = new TransformationCallback {
      override def onError(transformRequest: TransformRequest, throwable: Throwable): Unit = {
        println(throwable)
      }

      override def transformCompleted(transformResponse: TransformResponse): Unit = {
        println(transformResponse.getStatusMessage + "," + transformResponse.getTargets.get(0))
      }
    }

    t.sendTransform(path, MimeType.PDF, c)
  }
}