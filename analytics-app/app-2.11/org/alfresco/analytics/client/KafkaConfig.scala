package org.alfresco.analytics.client

import java.util.Properties

import com.typesafe.config.ConfigFactory
import org.alfresco.analytics.client.KafkaConfig._

/**
  * Created by sglover on 18/12/2015.
  */
// see https://github.com/vngrs/activator-hello-kafka
trait KafkaConfig extends Properties {

  private val consumerPrefixWithDot = consumerPrefix + "."
//  private val producerPrefixWithDot = producerPrefix + "."
//  private val allKeys = Seq(groupId, zookeeperConnect, brokers, serializer, partitioner, requiredAcks)
  private val allKeys = Seq(groupId, zookeeperConnect)

  lazy val typesafeConfig = ConfigFactory.load()

  allKeys.map { key =>
    if (typesafeConfig.hasPath(key))
//      put(key.replace(consumerPrefixWithDot, "").replace(producerPrefixWithDot, ""), typesafeConfig.getString(key))
      put(key.replace(consumerPrefixWithDot, ""), typesafeConfig.getString(key))
  }

  def getCustomString(key: String) = typesafeConfig.getString(key)
  def getCustomInt(key: String) = typesafeConfig.getInt(key)
}

object KafkaConfig {

  val consumerPrefix = "consumer"
//  val producerPrefix = "producer"

  //Consumer keys
  val groupId = s"$consumerPrefix.group.id"
  val zookeeperConnect = s"$consumerPrefix.zookeeper.connect"

  //example.producer.Producer keys
//  val brokers = s"$producerPrefix.metadata.broker.list"
//  val serializer = s"$producerPrefix.serializer.class"
//  val partitioner = s"$producerPrefix.partitioner.class"
//  val requiredAcks = s"$producerPrefix.request.required.acks"

  def apply() = new KafkaConfig {}
}