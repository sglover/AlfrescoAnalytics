package org.alfresco.analytics.client

import com.datastax.driver.core.policies.{DCAwareRoundRobinPolicy, DefaultRetryPolicy, TokenAwarePolicy}
import com.datastax.driver.core.{Cluster, KeyspaceMetadata, Session}

/**
  * Created by sglover on 18/12/2015.
  */
trait DB {
  protected val clear:Boolean = false

  if(clear)
  {
    println("Deleting keyspace " + DB.keyspace)
    deleteSchema
  }
  val keySpaceMetadata = getKeyspace
  createSchema

  def deleteSchema = {
    val keySpaceMetadata = getKeyspace
    if(keySpaceMetadata != null)
    {
      println("Deleting keyspace " + DB.keyspace)
      DB.session.execute("DROP KEYSPACE " + DB.keyspace + ";")
    }
  }

  def getKeyspace:KeyspaceMetadata = {
    var ks = DB.cluster.getMetadata().getKeyspace(DB.keyspace)
    if(ks == null)
    {
      println("Creating keyspace " + DB.keyspace)
      DB.session.execute("CREATE keyspace " + DB.keyspace + " WITH replication "
        + "= {'class':'SimpleStrategy', 'replication_factor':3};")
    }
    else
    {
      println("Not creating keyspace " + DB.keyspace)
    }
    ks = DB.cluster.getMetadata().getKeyspace(DB.keyspace)
    ks
  }

  def createSchema = {
    if(keySpaceMetadata.getTable("lines") == null)
    {
      println("Creating " + DB.keyspace + ".lines")
      DB.session.execute("CREATE TABLE " + DB.keyspace + ".lines (nodeid text, nodeversion int, id uuid, line text, "
        + "timestamp text, "
        + "PRIMARY KEY((nodeid, nodeversion), id));");
    }
    else
    {
      println("Not creating " + DB.keyspace + ".lines")
    }
    if(keySpaceMetadata.getTable("entities") == null)
    {
      println("Creating " + DB.keyspace + ".entities")
      DB.session.execute("CREATE TABLE " + DB.keyspace + ".entities (entitytype text, entity text, count int, nodeid text, nodeversion int, beginoffset int, endoffset int, probability double, context text, "
        + "PRIMARY KEY((entitytype, entity), nodeid, nodeversion));");
    }
    else
    {
      println("Not creating " + DB.keyspace + ".lines")
    }
    if(keySpaceMetadata.getTable("entitiesByNode") == null)
    {
      println("Creating " + DB.keyspace + ".entitiesByNode")
      DB.session.execute("CREATE TABLE " + DB.keyspace + ".entitiesByNode (entitytype text, entity text, count int, nodeid text, nodeversion int, beginoffset int, endoffset int, probability double, context text, "
        + "PRIMARY KEY((nodeid, nodeversion), entitytype, entity));");
    }
    else
    {
      println("Not creating " + DB.keyspace + ".entitiesByNode")
    }

    if(keySpaceMetadata.getTable("popular") == null)
    {
      println("Creating " + DB.keyspace + ".popular")
      DB.session.execute("CREATE TABLE " + DB.keyspace + ".popular (id text, count int, "
        + "PRIMARY KEY(id));");
    }
    else
    {
      println("Not creating " + DB.keyspace + ".popular")
    }

    if(keySpaceMetadata.getTable("popular_content") == null)
    {
      println("Creating " + DB.keyspace + ".popular_content")
      DB.session.execute("CREATE TABLE " + DB.keyspace + ".popular_content (nodeid text, count int, "
        + "PRIMARY KEY(nodeid));");
    }
    else {
      println("Not creating " + DB.keyspace + ".popular_content")
    }

    if(keySpaceMetadata.getTable("nodes_by_user") == null)
    {
      println("Creating " + DB.keyspace + ".nodes_by_user")
      DB.session.execute("CREATE TABLE " + DB.keyspace + ".nodes_by_user (username text, nodeids text, "
        + "PRIMARY KEY(username));");
    }
    else {
      println("Not creating " + DB.keyspace + ".nodes_by_user")
    }

    if(keySpaceMetadata.getTable("users_by_node") == null)
    {
      println("Creating " + DB.keyspace + ".users_by_node")
      DB.session.execute("CREATE TABLE " + DB.keyspace + ".users_by_node (nodeid text, usernames text, "
        + "PRIMARY KEY(nodeid));");
    }
    else {
      println("Not creating " + DB.keyspace + ".users_by_node")
    }

    if(keySpaceMetadata.getTable("by_count") == null)
    {
      println("Creating " + DB.keyspace + ".by_count")
      DB.session.execute("CREATE TABLE " + DB.keyspace + ".by_count (month text, nodeid text, count int,"
        + "PRIMARY KEY(month, nodeid));")
    }
    else {
      println("Not creating " + DB.keyspace + ".by_count")
    }

    if(keySpaceMetadata.getTable("activities_by_node") == null)
    {
      println(s"Creating ${DB.keyspace}.activities_by_node")
      DB.session.execute(s"CREATE TABLE ${DB.keyspace}.activities_by_node (nodeid text, type text, username text, timestamp timestamp,"
        + "PRIMARY KEY(nodeid, username));")
    }
    else {
      println(s"Not creating ${DB.keyspace}.activities_by_node")
    }

    if(keySpaceMetadata.getTable("activities_by_username") == null)
    {
      println(s"Creating ${DB.keyspace}.activities_by_username")
      DB.session.execute(s"CREATE TABLE ${DB.keyspace}.activities_by_username (nodeid text, type text, username text, timestamp timestamp,"
        + "PRIMARY KEY(username, nodeid));")
    }
    else {
      println(s"Not creating ${DB.keyspace}.activities_by_username")
    }
  }

  def shutdown() = {

  }
}

object DB {
  val keyspace = "alfresco"

  lazy val cluster = Cluster
    .builder()
    .addContactPoint("127.0.0.1")
    .withRetryPolicy(DefaultRetryPolicy.INSTANCE)
    .withLoadBalancingPolicy(
      new TokenAwarePolicy(new DCAwareRoundRobinPolicy()))
    .build()
  lazy val session:Session = cluster.connect()
  println("Connected to Cassandra")

  sys addShutdownHook {
    session.close()
    cluster.close()
  }
}