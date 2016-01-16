package org.alfresco.analytics

import java.util

import org.apache.chemistry.opencmis.client.api.Session
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl
import org.apache.chemistry.opencmis.commons.SessionParameter
import org.apache.chemistry.opencmis.commons.enums.BindingType

/**
  * Created by sglover on 07/01/2016.
  */
trait CMISOperations {
  val repoUsername:String
  val repoPassword:String

  private lazy val sessionFactory = SessionFactoryImpl.newInstance()
  lazy val cmisSession = getCMISSession()

  private def getCMISSession(): Session = {
    val parameters = new util.HashMap[String, String]()
    parameters.put(SessionParameter.BROWSER_URL, "http://localhost:8080/alfresco/api/-default-/public/cmis/versions/1.1/browser")
    parameters.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value())
    parameters.put(SessionParameter.REPOSITORY_ID, "-default-")
    parameters.put(SessionParameter.USER, repoUsername)
    parameters.put(SessionParameter.PASSWORD, repoPassword)

    // create session
    sessionFactory.createSession(parameters)
  }

  private def stripVersion(cmisObjectId:String) = {
    val idx = cmisObjectId.lastIndexOf(";")
    if(idx > -1)
    {
      cmisObjectId.substring(0, idx)
    }
    else
    {
      cmisObjectId
    }
  }
}
