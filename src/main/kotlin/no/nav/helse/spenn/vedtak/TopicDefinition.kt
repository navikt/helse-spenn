package no.nav.helse.spenn.vedtak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spenn.defaultObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory

private val strings = Serdes.String()
private val json = Serdes.serdeFrom(JsonSerializer(), JsonDeserializer())

val VEDTAK_SYKEPENGER = Topic(
        name = "privat-helse-spenn-utbetaling",
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
