package org.alfresco.analytics

import java.io.File
import java.util.UUID

/**
  * Created by sglover on 21/12/2015.
  */
object TempDirectory {

  /** Creates a new temporary directory and returns it's location. */
  def createTemporaryDirectory(prefix: String, suffix: String): File = {
    val base = new File(new File(System.getProperty("java.io.tmpdir")), prefix)
    val dir = new File(base, UUID.randomUUID().toString + suffix)
    dir.mkdirs()
    dir
  }

  def createTemporaryFile(prefix: String): File = {
    val base = new File(System.getProperty("java.io.tmpdir"))
    val uuid = UUID.randomUUID().toString()
    val file = new File(base, s"$prefix$uuid")
    file
  }

  /** Removes a directory (recursively). */
  def removeTemporaryDirectory(dir: File): Unit = {
    def recursion(f: File): Unit = {
      if (f.isDirectory) {
        f.listFiles().foreach(child ⇒ recursion(child))
      }
      f.delete()
    }
    recursion(dir)
  }
}