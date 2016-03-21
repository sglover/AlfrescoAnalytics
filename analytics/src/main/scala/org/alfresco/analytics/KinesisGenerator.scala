package org.alfresco.analytics

import java.nio.ByteBuffer

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.kinesis.model.{ResourceNotFoundException, DescribeStreamRequest, CreateStreamRequest, PutRecordRequest}

import scala.util.Random
import scala.util.control.Breaks
import scala.util.control.Breaks._

/**
  * Created by sglover on 26/02/2016.
  */
class KinesisGenerator(endpoint:String) {
  // Create the low-level Kinesis Client from the AWS Java SDK.
  val kinesisClient = new AmazonKinesisClient(new DefaultAWSCredentialsProviderChain())
  kinesisClient.setEndpoint(endpoint)

  def createStream(streamName: String, numShards: Int): Boolean = {
    val createStreamRequest = new CreateStreamRequest()
    createStreamRequest.setStreamName(streamName)
    createStreamRequest.setShardCount(numShards)

    kinesisClient.createStream(createStreamRequest)

    val describeStreamRequest = new DescribeStreamRequest()
    describeStreamRequest.setStreamName(streamName)

    val startTime = System.currentTimeMillis()
    val endTime = startTime + ( 10 * 60 * 1000 )

    Breaks.breakable {
      while (System.currentTimeMillis() < endTime) {
        try {
          Thread.sleep(20 * 1000)
        }
        catch
        {
          case e:Exception =>
        }

        try {
          val describeStreamResponse = kinesisClient.describeStream(describeStreamRequest)
          val streamStatus = describeStreamResponse.getStreamDescription().getStreamStatus()
          if (streamStatus.equals("ACTIVE")) {
            break
          }
          //
          // sleep for one second
          //
          try {
            Thread.sleep(1000)
          }
          catch {
            case e: Exception =>
          }
        }
        catch {
          case e: ResourceNotFoundException => {}
        }
      }
    }

    System.currentTimeMillis() >= endTime
  }

  def generate(stream: String,
               recordsPerSecond: Int,
               wordsPerRecord: Int): Seq[(String, Int)] = {

    val randomWords = List("spark", "you", "are", "my", "father")
    val totals = scala.collection.mutable.Map[String, Int]()

    println(s"Putting records onto stream $stream and endpoint $endpoint at a rate of" +
      s" $recordsPerSecond records per second and $wordsPerRecord words per record")

    // Iterate and put records onto the stream per the given recordPerSec and wordsPerRecord
    for (i <- 1 to 10) {
      // Generate recordsPerSec records to put onto the stream
      val records = (1 to recordsPerSecond.toInt).foreach { recordNum =>
        // Randomly generate wordsPerRecord number of words
        val data = (1 to wordsPerRecord.toInt).map(x => {
          // Get a random index to a word
          val randomWordIdx = Random.nextInt(randomWords.size)
          val randomWord = randomWords(randomWordIdx)

          // Increment total count to compare to server counts later
          totals(randomWord) = totals.getOrElse(randomWord, 0) + 1

          randomWord
        }).mkString(" ")

        // Create a partitionKey based on recordNum
        val partitionKey = s"partitionKey-$recordNum"

        // Create a PutRecordRequest with an Array[Byte] version of the data
        val putRecordRequest = new PutRecordRequest().withStreamName(stream)
          .withPartitionKey(partitionKey)
          .withData(ByteBuffer.wrap(data.getBytes()))

        // Put the record onto the stream and capture the PutRecordResult
        val putRecordResult = kinesisClient.putRecord(putRecordRequest)
      }

      // Sleep for a second
      Thread.sleep(1000)
      println("Sent " + recordsPerSecond + " records")
    }
    // Convert the totals to (index, total) tuple
    totals.toSeq.sortBy(_._1)
  }
}

object KinesisGenerator {
  def apply(endpoint:String):KinesisGenerator = {
    new KinesisGenerator(endpoint)
  }
}
