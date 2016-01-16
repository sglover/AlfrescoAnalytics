package org.alfresco.analytics

import spray.json.DefaultJsonProtocol

/**
  * Created by sglover on 22/12/2015.
  */
object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val windowedNodeCountFormat = jsonFormat4(WindowedNodeCount)
  implicit val popularEventFormat = jsonFormat1(PopularEvent)
  implicit val interestingNodesFormat = jsonFormat1(InterestingNodes)
  implicit val activityDataFormat = jsonFormat2(ActivityData)
  implicit val textTransformationDataFormat = jsonFormat2(TextTransformation)
  implicit val nodeAndPathFormat = jsonFormat2(NodeAndPath)
  implicit val entitiesAvailableFormat = jsonFormat1(EntitiesAvailable)
}
