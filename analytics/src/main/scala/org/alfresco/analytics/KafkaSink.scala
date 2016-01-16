package org.alfresco.analytics

import org.alfresco.events.types.Event
import org.apache.kafka.clients.producer.{ProducerRecord, KafkaProducer}
import collection.JavaConversions._

/**
  * Created by sglover on 22/12/2015.
  */
class KafkaSink(createProducer: () => KafkaProducer[String, String]) extends Serializable {

  lazy val producer = createProducer()

  def send(topic: String, event: String): Unit = {
    println(s"Sending $event to Kafka topic $topic")
    producer.send(new ProducerRecord(topic, event))
  }
}

object KafkaSink {
  def apply(config: Map[String, Object]): KafkaSink = {
    val f = () => {
      val producer = new KafkaProducer[String, String](config)

      sys.addShutdownHook {
        producer.close()
      }

      producer
    }
    new KafkaSink(f)
  }
}
