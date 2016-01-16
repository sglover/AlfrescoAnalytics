package org.alfresco.analytics

import java.util.Calendar

import org.alfresco.events.types.Event

/**
  * Created by sglover on 23/12/2015.
  */
object Constants {
  val keyspace = "alfresco"
  type count = Int
  type siteId = String
  type nodeId = String
  type userId = String

  def getYearMonth(timestamp:Long) = {
    val c = Calendar.getInstance()
    c.setTimeInMillis(timestamp)
    val year = c.get(Calendar.YEAR)
    val month = c.get(Calendar.MONTH)
    s"$year$month"
  }

  def getYearMonth(event:Event) = {
    val c = Calendar.getInstance()
    c.setTimeInMillis(event.getTimestamp)
    val year = c.get(Calendar.YEAR)
    val month = c.get(Calendar.MONTH)
    s"$year$month"
  }

  def calcCurrentYearMonth() = {
    val c = Calendar.getInstance()
    val year = c.get(Calendar.YEAR)
    val month = c.get(Calendar.MONTH)
    s"$year$month"
  }

  val currentYearMonth = calcCurrentYearMonth()
}
