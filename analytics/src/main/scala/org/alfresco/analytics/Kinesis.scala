package org.alfresco.analytics

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.kinesis.AmazonKinesisClient
import org.apache.spark.Logging
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Milliseconds, Duration}
import org.apache.spark.streaming.kinesis._
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream

/**
  * Created by sglover on 25/02/2016.
  */
class Kinesis() extends Sparky with Logging {

  val endpointUrl = "https://kinesis.eu-west-1.amazonaws.com"
  val streamName = "kinesisstream1"
  val appName = "sgkinesis1"

  // In this example, we're going to create 1 Kinesis Receiver/input DStream for each shard.
  // This is not a necessity; if there are less receivers/DStreams than the number of shards,
  // then the shards will be automatically distributed among the receivers and each receiver
  // will receive data from multiple shards.
  val numStreams = numShards

  // Spark Streaming batch interval
  val batchInterval = Milliseconds(2000)

  // Kinesis checkpoint interval is the interval at which the DynamoDB is updated with information
  // on sequence number of records that have been received. Same as batchInterval for this
  // example.
  val kinesisCheckpointInterval = batchInterval

  // Get the region name from the endpoint URL to save Kinesis Client Library metadata in
  // DynamoDB of the same region as the Kinesis stream
  val regionName = RegionUtils.getRegionByEndpoint(endpointUrl).getName()

  // Determine the number of shards from the stream using the low-level Kinesis Client
  // from the AWS Java SDK.
  val credentials = new DefaultAWSCredentialsProviderChain().getCredentials()
  require(credentials != null,
    "No AWS credentials found. Please specify credentials using one of the methods specified " +
      "in http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html")
  val kinesisClient = new AmazonKinesisClient(credentials)
  kinesisClient.setEndpoint(endpointUrl)
  val numShards = kinesisClient.describeStream(streamName).getStreamDescription().getShards().size

  // Create the Kinesis DStreams
  val kinesisStreams = (0 until numStreams).map { i =>
    KinesisUtils.createStream(streamingContext, appName, streamName, endpointUrl, regionName,
      InitialPositionInStream.LATEST, kinesisCheckpointInterval, StorageLevel.MEMORY_AND_DISK_2)
  }

  // Union all the streams
  val unionStreams = streamingContext.union(kinesisStreams)
  val words = unionStreams.flatMap { x =>
    x
  }

  streamingContext.start()
  streamingContext.awaitTermination()
}

object Kinesis {
  def apply():Kinesis = {
    new Kinesis()
  }
}
