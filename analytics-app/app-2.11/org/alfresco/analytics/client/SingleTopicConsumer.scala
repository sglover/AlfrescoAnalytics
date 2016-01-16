package org.alfresco.analytics.client

/**
  * Created by sglover on 18/12/2015.
  */
case class SingleTopicConsumer(topic: String) { //extends Consumer(List(topic)) {
//  private lazy val consumer = KafkaConsume.create(config)
//  val threadNum = 1
//
//  private lazy val consumerMap = consumer.createMessageStreams(Map(topic -> threadNum))
//  private lazy val stream = consumerMap.getOrElse(topic, List()).head
//
//  override def read(): Stream[String] = Stream.cons(new String(stream.head.message()), read())
}
