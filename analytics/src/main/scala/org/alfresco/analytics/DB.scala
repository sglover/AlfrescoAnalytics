package org.alfresco.analytics

import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy
import com.datastax.driver.core.policies.TokenAwarePolicy
import com.datastax.driver.core.policies.DefaultRetryPolicy
import com.datastax.driver.core.{Session, Cluster, KeyspaceMetadata}

/**
 * @author sglover
 */
trait DB {
  protected val clear:Boolean = false
//  val keyspace = "alfresco"

  @transient lazy val cluster = Cluster
    .builder()
    .addContactPoint("127.0.0.1")
    .withRetryPolicy(DefaultRetryPolicy.INSTANCE)
    .withLoadBalancingPolicy(
      new TokenAwarePolicy(new DCAwareRoundRobinPolicy()))
    .build()
  @transient lazy val session:Session = cluster.connect()

  println("Connected to Cassandra:" + clear)

  if(clear)
  {
    println("Deleting keyspace " + Constants.keyspace)
    deleteSchema
  }
  @transient val keySpaceMetadata = getKeyspace
  createSchema

  def deleteSchema = {
    val keySpaceMetadata = getKeyspace
    if(keySpaceMetadata != null)
    {
      println("Deleting keyspace " + Constants.keyspace)
      session.execute("DROP KEYSPACE " + Constants.keyspace + ";")
    }
  }

  def getKeyspace:KeyspaceMetadata = {
    var ks = cluster.getMetadata().getKeyspace(Constants.keyspace)
    if(ks == null)
    {
        println("Creating keyspace " + Constants.keyspace)
        session.execute("CREATE keyspace " + Constants.keyspace + " WITH replication "
                + "= {'class':'SimpleStrategy', 'replication_factor':3};")
    }
    else
    {
      println("Not creating keyspace " + Constants.keyspace)
    }
    ks = cluster.getMetadata().getKeyspace(Constants.keyspace)
    ks
  }

  def createSchema = {
//    if(keySpaceMetadata.getTable("events") == null)
//    {
//      println(s"Creating $keyspace.events")
//      session.execute(s"CREATE TABLE $keyspace.events (nodeid text, nodeversion int, id uuid, line text, "
//        + "timestamp text, "
//        + "PRIMARY KEY((nodeid, nodeversion), id));")
//    }
//    else
//    {
//      println(s"Not creating $keyspace.events")
//    }

    if(keySpaceMetadata.getTable("lines") == null)
    {
      println("Creating " + Constants.keyspace + ".lines")
      session.execute("CREATE TABLE " + Constants.keyspace + ".lines (nodeid text, nodeversion int, id uuid, line text, "
        + "timestamp text, "
        + "PRIMARY KEY((nodeid, nodeversion), id));")
    }
    else
    {
      println(s"Not creating ${Constants.keyspace}.lines")
    }

    if(keySpaceMetadata.getTable("ratings") == null)
    {
      println("Creating " + Constants.keyspace + ".ratings")
      session.execute("CREATE TABLE " + Constants.keyspace + ".ratings (yearmonth text, nodeid text, userid text, rating int, "
                + "PRIMARY KEY((yearmonth), nodeid, userid));")
    }
    else
    {
      println(s"Not creating ${Constants.keyspace}.ratings")
    }

    // TODO need to decide on partition key - what should it be here?
    if(keySpaceMetadata.getTable("ratings_by_user") == null)
    {
      println("Creating " + Constants.keyspace + ".ratings_by_user")
      session.execute("CREATE TABLE " + Constants.keyspace + ".ratings_by_user (yearMonth text, type text, userid text, nodeid text, rating int, timestamp int, "
        + "PRIMARY KEY((yearMonth, type), userid, nodeid, rating)) WITH CLUSTERING ORDER BY (userid ASC, nodeid ASC, rating DESC);")
    }
    else
    {
      println(s"Not creating ${Constants.keyspace}.ratings_by_user")
    }

    if(keySpaceMetadata.getTable("entities_by_type") == null)
    {
        println(s"Creating ${Constants.keyspace}.entities_by_type")
        session.execute(s"CREATE TABLE ${Constants.keyspace}.entities_by_type (entitytype text, entity text, timestamp timestamp, count int, nodeid text, nodeversion int, beginoffset bigint, endoffset bigint, probability double, context text, "
                + "PRIMARY KEY(entitytype, timestamp, entity, nodeid, nodeversion));")
    }
    else
    {
      println(s"Not creating ${Constants.keyspace}.entities_by_type")
    }

    if(keySpaceMetadata.getTable("entities_by_node") == null)
    {
      println(s"Creating ${Constants.keyspace}.entities_by_node")
        session.execute(s"CREATE TABLE ${Constants.keyspace}.entities_by_node (entitytype text, entity text, count int, timestamp timestamp, nodeid text, nodeversion int, beginoffset bigint, endoffset bigint, probability double, context text, "
                + "PRIMARY KEY((nodeid, nodeversion), timestamp, entitytype, entity));")
    }
    else
    {
      println(s"Not creating ${Constants.keyspace}.entities_by_node")
    }

    if(keySpaceMetadata.getTable("popular") == null)
    {
      println("Creating " + Constants.keyspace + ".popular")
      session.execute("CREATE TABLE " + Constants.keyspace + ".popular (id text, count int, "
        + "PRIMARY KEY(id));")
    }
    else
    {
      println("Not creating " + Constants.keyspace + ".popular")
    }

    if(keySpaceMetadata.getTable("popular_content") == null)
    {
      println("Creating " + Constants.keyspace + ".popular_content")
      session.execute("CREATE TABLE " + Constants.keyspace + ".popular_content (yearmonth text, timestamp timestamp, count int, nodeid text, "
        + "PRIMARY KEY(yearmonth, timestamp, count)) WITH CLUSTERING ORDER BY (timestamp ASC, count DESC);")
    }
    else {
      println("Not creating " + Constants.keyspace + ".popular_content")
    }

    if(keySpaceMetadata.getTable("popular_sites") == null)
    {
      println("Creating " + Constants.keyspace + ".popular_sites")
      session.execute("CREATE TABLE " + Constants.keyspace + ".popular_sites (yearmonth text, count int, siteid text, "
        + "PRIMARY KEY(yearmonth, count, siteid)) WITH CLUSTERING ORDER BY (count DESC);")
    }
    else {
      println("Not creating " + Constants.keyspace + ".popular_sites")
    }

    if(keySpaceMetadata.getTable("nodes_by_user") == null)
    {
      println("Creating " + Constants.keyspace + ".nodes_by_user")
      session.execute("CREATE TABLE " + Constants.keyspace + ".nodes_by_user (yearmonth text, username text, nodeid text, "
        + "PRIMARY KEY(yearmonth, username, nodeid));")
    }
    else {
      println("Not creating " + Constants.keyspace + ".nodes_by_user")
    }

    if(keySpaceMetadata.getTable("users_by_node") == null)
    {
      println("Creating " + Constants.keyspace + ".users_by_node")
      session.execute("CREATE TABLE " + Constants.keyspace + ".users_by_node (yearmonth text, nodeid text, username text, "
        + "PRIMARY KEY(yearmonth, nodeid, username));")
    }
    else {
      println("Not creating " + Constants.keyspace + ".users_by_node")
    }

//    if(keySpaceMetadata.getTable("by_count") == null)
//    {
//      println("Creating " + keyspace + ".by_count")
//      session.execute("CREATE TABLE " + keyspace + ".by_count (month text, nodeid text, count int,"
//        + "PRIMARY KEY(month, nodeid));")
//    }
//    else {
//      println("Not creating " + keyspace + ".by_count")
//    }

//    if(keySpaceMetadata.getTable("counts_by_node") == null)
//    {
//      println("Creating " + keyspace + ".counts_by_node")
//      session.execute("CREATE TABLE " + keyspace + ".counts_by_node (nodeid text, count int,"
//        + "PRIMARY KEY(nodeid));");
//    }
//    else {
//      println("Not creating " + keyspace + ".counts_by_node")
//    }

    if(keySpaceMetadata.getTable("user_node_interest") == null)
    {
      println("Creating " + Constants.keyspace + ".user_node_interest")
      session.execute("CREATE TABLE " + Constants.keyspace + ".user_node_interest (username text, fromusername text, nodeid text,"
        + " yearmonth text,"
        + "PRIMARY KEY(username, nodeid, fromusername, yearmonth));");
    }
    else {
      println("Not creating " + Constants.keyspace + ".user_node_interest")
    }
  }

  def createUsersByNodeTable(name:String): Unit = {
    if(keySpaceMetadata.getTable(name) == null)
    {
      println(s"Creating ${Constants.keyspace}.$name")
      session.execute(s"CREATE TABLE ${Constants.keyspace}.$name (nodeid text, username text, "
        + "PRIMARY KEY(nodeid, username));")
    }
    else {
      println(s"Not creating ${Constants.keyspace}.$name")
    }
  }

  def createNodesByUserTable(name:String): Unit = {
    if (keySpaceMetadata.getTable(name) == null) {
      println(s"Creating ${Constants.keyspace}.$name")
      session.execute(s"CREATE TABLE ${Constants.keyspace}.$name (nodeid text, username text, "
        + "PRIMARY KEY(username, nodeid));")
    }
    else {
      println(s"Not creating ${Constants.keyspace}.$name")
    }
  }

  def shutdown() = {
    session.close()
    cluster.close()
  }
}