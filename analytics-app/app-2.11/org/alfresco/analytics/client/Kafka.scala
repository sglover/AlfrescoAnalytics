package org.alfresco.analytics.client

/**
  * Created by sglover on 18/12/2015.
  */

class Kafka(partition: Int = 0, offset: Long = 0L, fetchSize: Int = 100) { //extends Actor with DB with ActorLogging {
////
////  protected val kafkaConfig = KafkaConfig()
////  protected val config = new ConsumerConfig(kafkaConfig)
////
////  private val clientId = kafkaConfig.getCustomString("consumer.clientId")
//
//  implicit val timeout = Timeout(5.seconds)
//
//  implicit val ctx = context.dispatcher
//
//  override val supervisorStrategy =
//    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
//      case _: ActorInitializationException => Stop
//      case _: IllegalArgumentException => Stop
//      case _: IllegalStateException    => Restart
//      case _: TimeoutException         => Escalate
//      case _: Exception                => Escalate
//    }
//
//  val activityByNode = DB.session.prepare(
//    s"INSERT INTO ${DB.keyspace}.activities_by_node (nodeId, type, username, timestamp) VALUES(?, ?, ?, ?);")
//
//  val activityByUsername = DB.session.prepare(
//    s"INSERT INTO ${DB.keyspace}.activities_by_username (nodeId, type, username, timestamp) VALUES(?, ?, ?, ?);")
//
//  implicit val actorSystem = ActorSystem("ReactiveKafka")
//  implicit val materializer = ActorMaterializer()
//
//  class KafkaActor extends Actor {
//    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
//      case exception => Resume // Your custom error handling
//    }
//
//    def createSupervisedSubscriberActor() = {
//      val kafka = new ReactiveKafka()
//
//      // subscriber
//      val subscriberProperties = ProducerProperties(
//        brokerList = "localhost:9092",
//        topic = "",
//        clientId = "",
//        encoder = new StringEncoder
//      )
//      val subscriberActorProps: Props = kafka.producerActorProps(subscriberProperties)
//      context.actorOf(subscriberActorProps)
//    }
//
//    override def receive: Receive = {
//      case _ => // ignore
//    }
//  }
//


//  val publisher: Publisher[StringKafkaMessage] = kafka.consume(ConsumerProperties(
//    brokerList = "localhost:9092",
//    zooKeeperHost = "localhost:2181",
//    topic = "lowercaseStrings",
//    groupId = "groupName",
//    decoder = new StringDecoder()
//  ))
//  val subscriber: Subscriber[String] = kafka.publish(ProducerProperties(
//    brokerList = "localhost:9092",
//    topic = "uppercaseStrings",
//    encoder = new StringEncoder()
//  ))
//
//  Source(publisher).map(_.message().toUpperCase).to(Sink(subscriber)).run()
//
//
//
//
//
//
////  val kafka = new ReactiveKafka()
////
////  // subscriber
////  val subscriberProperties = ProducerProperties(
////    brokerList = "localhost:9092",
////    topic = "alfresco.repo.activities",
////    clientId = "analytics",
////    encoder = new StringEncoder
////  )
////  val subscriberActorProps: Props = kafka.producerActorProps(subscriberProperties)
////  val activityEventsConsumer = Akka.system().actorOf(subscriberActorProps, name = "activityEventsConsumer")
//
//
//
//
//
//
//
//
//
//
//  val typesafeConfig = ConfigFactory.load()
//  val topic = typesafeConfig.getString("kafka.consumer.topic")
//  val groupId = typesafeConfig.getString("kafka.consumer.group.id")
//  val zookeeperConnect = typesafeConfig.getString("kafka.consumer.zookeeper.connect")
//  val autoOffsetReset = typesafeConfig.getString("kafka.consumer.auto.offset.reset")
//
//  val props = new Properties()
//  props.put("group.id", groupId)
//  props.put("zookeeper.connect", zookeeperConnect)
//  props.put("auto.offset.reset", autoOffsetReset)
//
//  val config = new ConsumerConfig(props)
//
//  val connector = Consumer.create(config)
//
//  val filterSpec = new Whitelist(topic)
//
//  val stream = connector.createMessageStreamsByFilter(filterSpec, 1, new StringDecoder(), new StringDecoder()).head
//  for(messageAndTopic <- stream) {
//    try {
//      val event: Event = Serializer.deserialize(messageAndTopic.message)
//      println("kafka activity event=" + event)
//      event match {
//        case ae:ActivityEvent => {
//          DB.session.execute(activityByNode.bind(ae.getNodeId(), ae.getType(), ae.getUsername(),
//            new Date(ae.getTimestamp)))
//          DB.session.execute(activityByUsername.bind(ae.getNodeId(), ae.getType(), ae.getUsername(),
//            new Date(ae.getTimestamp)))
//        }
//        case something =>
//      }
//    } catch {
//      case e: Throwable =>
//        if (true) { //this is objective even how to conditionalize on it
//          println("Error processing message, skipping this message: ", e)
//        } else {
//          throw e
//        }
//    }
//  }
//
//
//
//
//  //  val simpleConsumer = new SimpleConsumer(
////    kafkaConfig.getCustomString("consumer.host"),
////    kafkaConfig.getCustomInt("consumer.port"),
////    kafkaConfig.getCustomInt("consumer.timeOut"),
////    kafkaConfig.getCustomInt("consumer.bufferSize"),
////    clientId)
////
////  simpleConsumer.
////  def read(): Iterable[String] = {
////    val fetchRequest = new FetchRequestBuilder().clientId(clientId)
////    for (topic <- topics) {
////      fetchRequest.addFetch(topic, partition, offset, fetchSize)
////    }
////
////    val fetchResponse = simpleConsumer.fetch(fetchRequest.build())
////    fetchResponse.data.values.flatMap { topic =>
////      topic.messages.toList.map { mao =>
////        val payload = mao.message.payload
////
////        //ugliest part of the code. Thanks to kafka
////        val data = Array.fill[Byte](payload.limit)(0)
////        payload.get(data)
////        new String(data)
////      }
////    }
////  }
//
////  private val kafkaConsumerConfig =
////    new ConsumerConfig(ConfigFactory.load().getConfig("kafka.consumer").entrySet().foldRight(new Properties()) {
////      (item, props) =>
////        props.setProperty(item.getKey, item.getValue.unwrapped().toString)
////        props
////    })
////
////  private def connect(config: ConsumerConfig) = Consumer.create(config)
////
////  private def consume(topic: String, connection: ConsumerConnector) =
////    connection.createMessageStreamsByFilter(new Whitelist(topic), 1, new StringDecoder, new StringDecoder).headOption.map(_.toStream)
////
////  def listenTo(topic: String) = WebSocket.using[String] { _ =>
////
////    val connection = connect(kafkaConsumerConfig)
////    var connected = true
////
////    val endOnDisconnection = Iteratee.foreach[String](println).map { _ =>
////      connection.shutdown()
////      connected = false
////    }
//
////  val kafkaParams = Map("metadata.broker.list" -> "mykafka:9092")
////
////  val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
////    streamingContext, kafkaParams, topics)
////
////  // messages.foreachRDD { rdd, time: Time) => {
////  messages.foreachRDD { rdd =>
////
////    if (rdd.toLocalIterator.nonEmpty) {
////
////      val events = rdd.map{case (_,v) => v}
////      println("We have "+events.count()+ " events");
////      //      events.foreach(println)
////      events
////        .map { msg => {
////          val event: Event = Serializer.deserialize(msg)
////          (event.get, 1)
////        }
////        }
////        .reduceByKey {
////          (v1, v2) => v1 + v2
////        }
////        .map {
////          x => NodeCount(yearMonth, x._1, x._2)
////        }
////        .saveToCassandra(keyspace, "popular_content", SomeColumns("yearmonth", "nodeid", "count"))
////
////    }
////
////  }

}

object Kafka {
  def apply() = {
    new Kafka()
  }
}