package org.alfresco.analytics

/**
  * Created by sglover on 22/12/2015.
  */

import com.fasterxml.jackson.databind.DeserializationFeature
import org.alfresco.events.types.{ActivityEvent, Event}
import java.io.{InputStream, StringWriter}
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import org.apache.commons.io.IOUtils;

/**
  * @author sglover
  */
object Serializer {
  val mapper = new QpidJsonBodyCleanerObjectMapper()
  mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def deserializeAsActivityEvent(json: String): ActivityEvent = {
    mapper.readValue(json, classOf[Object]).asInstanceOf[ActivityEvent]
  }

  def deserializeAsEvent(json: String): Event = {
    mapper.readValue(json, classOf[Object]).asInstanceOf[Event]
  }

  def serializeAsEvent(event: Event): String = {
    mapper.writeValueAsString(event)
  }

  def deserializeAsAnalyticsEvent(json: String): AnalyticsEvent = {
    mapper.readValue(json, classOf[Object]).asInstanceOf[AnalyticsEvent]
  }

  def serializeAsAnalyticsEvent(event: AnalyticsEvent): String = {
    mapper.writeValueAsString(event)
  }
}

/**
  * Extension of ObjectMapper which cleans erroneous characters apparently
  * added by the Qpid library before the start of a JSON object.
  */
class QpidJsonBodyCleanerObjectMapper extends ObjectMapper
{
  val DEFAULT_ENCODING = "utf8"

  def readValue[T: Manifest](inputStream:InputStream, valueType:Class[T]): T = {
    try
    {
      // Try to unmarshal normally
      if (inputStream.markSupported())
      {
        inputStream.mark(1024 * 512)
      }
      return super.readValue(inputStream, valueType);
    }
    catch
      {
        case e:JsonParseException => {
          if (!inputStream.markSupported()) {
            // We can't reset this stream, bail out
            throw e
          }
          // Reset the stream
          inputStream.reset()
        }
      }
    // Clean the message body and try again
    val writer = new StringWriter()
    IOUtils.copy(inputStream, writer, DEFAULT_ENCODING)
    var content = writer.toString
    content = content.substring(content.indexOf("{"), content.length())
    readValue(content, valueType)
  }
}