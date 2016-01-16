package org.alfresco.analytics

import java.util.Date

/**
  * Created by sglover on 09/01/2016.
  */
case class Entity(nodeid:String, nodeversion:Int, timestamp:Date, entitytype:String, entity:String, beginOffset:Long, endOffset:Long, probability:Double, context:String, count:Int = 1)
