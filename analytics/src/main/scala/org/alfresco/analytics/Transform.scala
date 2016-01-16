package org.alfresco.analytics

/**
  * Created by sglover on 23/12/2015.
  */

import akka.actor.ActorSystem
import org.alfresco.transformation.api.options.TransformationOptions
import org.alfresco.transformation.api.{ContentReference, MimeType, TransformRequest, TransformResponse}
import org.alfresco.transformation.client.{TransformationCallback, TransformationClient}
import org.alfresco.transformation.config.ClientConfig

import scala.collection.JavaConversions._

object Transform {
  def apply(actorSystem:ActorSystem) = {
    new Transform(actorSystem)
  }
}

/**
  * Created by sglover on 11/12/2015.
  */
class Transform(actorSystem:ActorSystem) {
  val routers = List("localhost:2551")
    val config:ClientConfig = new ClientConfig("localhost", 2192, routers)
//  val config:ClientConfig = new ClientConfig("localhost", null, routers)
//  val client:TransformationClient = new TransformationClient(config, actorSystem)
val client:TransformationClient = new TransformationClient(config)

  def closeClient() =
  {
    if(client != null)
    {
      client.disconnect();
    }
  }

  def sendTransform(path:String, mimeType:MimeType, callback:TransformationCallback) =
  {
    val ref = new ContentReference(path, mimeType)
    val options:TransformationOptions = new TransformationOptions()
    options.setMimetype(MimeType.TEXT)
    println(s"Send transform for $path")
    client.transform(new TransformRequest(ref, options), callback)
    println(s"Sent transform for $path")
  }

  class Callback extends TransformationCallback
  {
    @Override
    def transformCompleted(res:TransformResponse)
    {
      var msg:String = res.getStatus().toString();
      val source:ContentReference = res.getRequest().getSource();
      msg += "\n  source: [" + source.getMimetype() + "] " + res.getRequest().getSource().getPath();
      msg += "\n  time: " + res.getTimeTaken();
      msg += "\n  transformer: " + res.getTransformerId();
      println(msg);
    }

    @Override
    def onError(req:TransformRequest, e:Throwable)
    {
      var msg:String = "EXCEPTION " + e.getMessage();
      val source:ContentReference = req.getSource();
      msg += "\n  source: [" + source.getMimetype() + "] " + source.getPath();
      println(msg)
    }
  }
}

object TransformTest {
  def main(args:Array[String]) = {
    val path = args(0)
    //, config
    val actorSystem = ActorSystem.create("TransformTest")
    val t = new Transform(actorSystem)
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