package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory

val defaultObjectMapper: ObjectMapper = jacksonObjectMapper()
                                          .registerModule(JavaTimeModule())
                                          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private val strings = Serdes.String()
private val json = Serdes.serdeFrom(JsonSerializer(), JsonDeserializer())

val VEDTAK_SYKEPENGER = Topic(
        name = "aapen-helse-sykepenger-vedtak",
        keySerde = strings,
        valueSerde = json
)

class JsonSerializer: Serializer<JsonNode> {
   private val log = LoggerFactory.getLogger("JsonSerializer")
   override fun serialize(topic: String?, data: JsonNode?): ByteArray? {
      return data?.let {
         try {
            defaultObjectMapper.writeValueAsBytes(it)
         }
         catch(e: Exception) {
            log.warn("Could not serialize JsonNode",e)
            null
         }
      }
   }

   override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) { }
   override fun close() { }

}

class JsonDeserializer: Deserializer<JsonNode> {
   private val log = LoggerFactory.getLogger("JsonDeserializer")

   override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) { }

   override fun deserialize(topic: String?, data: ByteArray?): JsonNode? {
      return data?.let {
         try {
            defaultObjectMapper.readTree(it)
         } catch (e: Exception) {
            log.warn("Not a valid json",e)
            null
         }
      }
   }

   override fun close() { }

}
