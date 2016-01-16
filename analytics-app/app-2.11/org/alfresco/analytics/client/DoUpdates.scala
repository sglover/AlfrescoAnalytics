package org.alfresco.analytics.client

import java.io.{FileInputStream, InputStream}
import java.util.UUID
import collection.JavaConversions._

import org.springframework.social.alfresco.api.entities.Role

import scala.collection.mutable

/**
  * Created by sglover on 02/01/2016.
  */
case class DoUpdates(siteId:String) {
  val docLibraryPath = s"/Sites/$siteId/documentLibrary"

  val folderName = UUID.randomUUID().toString
  val parentPath = s"$docLibraryPath/$folderName"
  val folderProperties = Map[String, Object]()

  val files = Files("/Users/sglover/src/git/AlfrescoExtensions/analytics-app/files")

  val f1 = files.randomFile()
  val filename1 = f1.filename
  val properties1 = Map[String, Object]()
  val stream1 = new FileInputStream(f1.path)
  val mimeType1 = f1.mimeType
  val size1 = f1.size

  val f2 = files.randomFile()
  val filename2 = f2.filename
  val properties2 = Map[String, Object]()
  val stream2 = new FileInputStream(f2.path)
  val mimeType2 = f2.mimeType
  val size2 = f2.size

  object AdminUpdates extends ClientSession("admin", "admin")
  {
    def apply() = {
      createUser("sglover", "Steve", "Glover", "steven.glover@alfresco.com", "password")
      createUser("bob", "Bob", "Ajob", "steven.glover@alfresco.com", "password")
      createUser("alice", "Alice", "Ajob", "steven.glover@alfresco.com", "password")
    }
  }

  object SteveUpdates extends ClientSession("sglover", "password")
  {
    def apply() = {
      createSite(siteId, siteId)
      addSiteMember(siteId, "bob", Role.SiteContributor)
      addSiteMember(siteId, "alice", Role.SiteContributor)
      addFolder(docLibraryPath, folderName, folderProperties)
      addDocument(parentPath, filename1, properties1, mimeType1, size1, stream1)
      like(s"$parentPath/$filename1")
    }
  }

  object BobUpdates extends ClientSession("bob", "password")
  {
    def apply() = {
      addDocument(parentPath, filename2, properties2, mimeType2, size2, stream2)
      like(s"$parentPath/$filename1")
      like(s"$parentPath/$filename2")
    }
  }

  object AliceUpdates extends ClientSession("alice", "password")
  {
    def apply() = {
      like(s"$parentPath/$filename1")
      like(s"$parentPath/$filename2")
    }
  }

  AdminUpdates()
  SteveUpdates()
  BobUpdates()
  AliceUpdates()
}

object DoUpdates {
  def main(args: Array[String]): Unit = {
    DoUpdates(args(0))
  }
}