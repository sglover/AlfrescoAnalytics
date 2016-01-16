package org.alfresco.analytics

import org.joda.time.DateTime

/**
  * Created by sglover on 09/01/2016.
  */
case class Sentence(nodeid:String, nodeversion:Int, offset:Long, line:String, timestamp:DateTime)