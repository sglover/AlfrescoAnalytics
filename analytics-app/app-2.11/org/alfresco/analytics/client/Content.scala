package org.alfresco.analytics.client

import com.google.common.io.Files
import org.alfresco.checksum.ChecksumServiceImpl
import org.alfresco.checksum.dao.mongo.MongoChecksumDAO
import org.alfresco.contentstore.dao.mongo.MongoNodeUsageDAO
import org.alfresco.contentstore.{CassandraSession, CassandraContentStore}
import org.alfresco.extensions.common.identity.ServerIdentityImpl

/**
  * Created by sglover on 30/03/2016.
  */
class Content {
  val rootDirectory = Files.createTempDir()

  System.out.println("rootDirectory = " + rootDirectory)

  val serverIdentity = new ServerIdentityImpl("localhost", 8080, "test")
  val time = System.currentTimeMillis()

  val checksumDAO = new MongoChecksumDAO(db, "checksums" + time);
  val nodeUsageDAO = new MongoNodeUsageDAO()(db, "nodeUsage" + time, serverIdentity);

  val checksumService = new ChecksumServiceImpl(checksumDAO, 5);

  val cassandraSession = new CassandraSession("localhost", true);
  val cs = new CassandraContentStore(cassandraSession)
}
