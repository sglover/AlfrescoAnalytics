package org.alfresco.analytics.client

import play.{Application, GlobalSettings}

/**
 * Application global object, used here to schedule jobs on application start-up.
 *
 * @author sglover
 */
class Global extends GlobalSettings {
  override def onStart(app: Application) {
    Actors.createActors
   // Kafka()
  }
}