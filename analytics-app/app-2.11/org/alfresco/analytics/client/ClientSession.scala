package org.alfresco.analytics.client

import java.io.InputStream
import java.math.BigInteger
import java.util

import org.apache.chemistry.opencmis.client.api.{Folder, Document, Session}
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl
import org.apache.chemistry.opencmis.commons.{PropertyIds, SessionParameter}
import org.apache.chemistry.opencmis.commons.data.ContentStream
import org.apache.chemistry.opencmis.commons.enums.{VersioningState, BindingType}
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.io.IOUtils
import org.springframework.social.alfresco.api.entities.Role
import org.springframework.social.alfresco.api.entities.Site.Visibility
import org.springframework.social.alfresco.api.{Alfresco, CMISEndpoint}
import org.springframework.social.alfresco.api.impl.ConnectionDetails
import org.springframework.social.alfresco.connect.BasicAuthAlfrescoConnectionFactory
import org.springframework.social.alfresco.connect.exception.AlfrescoException
import org.springframework.social.connect.ConnectionData
import scala.collection.mutable
import scala.collection.JavaConversions._

/**
  * Created by sglover on 02/01/2016.
  */
case class ClientSession(username:String, password:String) {
  private lazy val sessionFactory = SessionFactoryImpl.newInstance()
  private lazy val publicAPI = getPublicApiSession()
  private lazy val cmisSession = getCMISSession()

  private def getCMISSession(): Session = {
    val parameters = new util.HashMap[String, String]()
    parameters.put(SessionParameter.BROWSER_URL, "http://localhost:8080/alfresco/api/-default-/public/cmis/versions/1.1/browser")
    parameters.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value())
    parameters.put(SessionParameter.REPOSITORY_ID, "-default-")
    parameters.put(SessionParameter.USER, username)
    parameters.put(SessionParameter.PASSWORD, password)

    // create session
    sessionFactory.createSession(parameters)
  }

  private def getPublicApiSession(): Alfresco = {
    val repo = new ConnectionDetails("http", "localhost", 8080, username, password, 2, 2000, 2000, 2000)
    val sync = new ConnectionDetails("http", "localhost", 9090, username, password, 2, 2000, 2000, 2000)
    val publicApiFactory = new BasicAuthAlfrescoConnectionFactory(repo, sync)
    publicApiFactory.createConnection().getApi()
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

  trait command

  object like extends command {
    def apply(path:String) = {
      val cmisObject = cmisSession.getObjectByPath(path)
      val nodeId = stripVersion(cmisObject.getId)
      publicAPI.rateNode("-default-", nodeId, true)
    }
  }

  object addDocument extends command {
    def apply(parentPath:String, filename: String, properties:Map[String, _], mimeType: String,
              length:Long, stream: InputStream) = {
      cmisSession.getObjectByPath(parentPath) match {
        case f:Folder => {
          val contentStream = new ContentStreamImpl(filename, BigInteger.valueOf(length), mimeType, stream)
          f.createDocument(properties
            + (PropertyIds.OBJECT_TYPE_ID -> properties.getOrElse(PropertyIds.OBJECT_TYPE_ID, "cmis:document"))
            + (PropertyIds.NAME -> properties.getOrElse(PropertyIds.NAME, filename)),
            contentStream, VersioningState.MAJOR)
        }
      }
    }
  }

  object addFolder extends command {
    def apply(parentPath:String, folderName: String, properties:Map[String, _]) = {
      cmisSession.getObjectByPath(parentPath) match {
        case f:Folder => {
          f.createFolder(properties
            + (PropertyIds.OBJECT_TYPE_ID -> properties.getOrElse(PropertyIds.OBJECT_TYPE_ID, "cmis:folder"))
            + (PropertyIds.NAME -> properties.getOrElse(PropertyIds.NAME, folderName)))
        }
      }
    }
  }

  object getContent extends command {
    def apply(path:String) = {
      cmisSession.getObjectByPath(path) match {
        case d: Document => {
          val cs = d.getContentStream()
          val buf = Array[Byte]()
          IOUtils.read(cs.getStream, buf)
        }
      }
    }
  }

  object addComment extends command {
    def apply(path:String, comment:String) = {
      cmisSession.getObjectByPath(path) match {
        case d: Document => {
          val nodeId = stripVersion(d.getId())
          publicAPI.createComment("-default-", nodeId, comment)
        }
      }
    }
  }

  object createSite extends command {
    def apply(siteId:String, title:String) = {
      try {
        val site = publicAPI.getSite(siteId, "-default-")
        if (site == null) {
          publicAPI.createSite("-default-", siteId, "testSitePreset", title, title,
            Visibility.PUBLIC)
        }
      }
      catch {
        case e: AlfrescoException => {
          e.getStatusCode().value() match {
            case HttpStatus.SC_NOT_FOUND => {
              publicAPI.createSite("-default-", siteId, "testSitePreset", title, title,
                Visibility.PUBLIC)
            }
            case _ => {

            }
          }
        }
      }
    }
  }

  object addSiteMember extends command {
    def apply(siteId: String, username: String, role: Role) = {
      try
      {
        val personSite = publicAPI.getSite("-default-", username, siteId)
        if(personSite == null)
        {
          publicAPI.addMember("-default-", siteId, username, role)
        }
      }
      catch
      {
        case e:AlfrescoException => {
          e.getStatusCode().value() match {
            case HttpStatus.SC_NOT_FOUND => {
              publicAPI.addMember("-default-", siteId, username, role)
            }
          }
        }
      }
    }
  }

  object createUser extends command {
    def apply(username:String, firstName:String, lastName:String, email:String, password:String) = {
      try
      {
        val person = publicAPI.getPerson("-default-", username)
        if(person == null)
        {
          publicAPI.createPerson("-default-", username, firstName, lastName, email, password)
        }
      }
      catch
      {
        case e:AlfrescoException => {
          e.getStatusCode().value() match {
            case HttpStatus.SC_NOT_FOUND => {
              publicAPI.createPerson("-default-", username, firstName, lastName, email, password)
            }
          }
        }
      }
    }
  }
}

//object ClientSession {
////  implicit def int2LineBuilder(x: Symbol) = LineBuilder(i)
//
//  def apply(username:String, password:String) = {
//    new ClientSession(username, password)
//  }
//}
