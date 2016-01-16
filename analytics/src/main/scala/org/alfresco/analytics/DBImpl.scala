package org.alfresco.analytics

/**
 * @author sglover
 */
class DBImpl(override protected val clear:Boolean) extends DB

object DBImpl {
  def apply(clear:Boolean):DBImpl = {
    new DBImpl(clear)
  }
}