package io.gamov.parkflow.entry

import io.gamov.parkflow.entry.routes.entryRoutes
import io.gamov.parkflow.events.VehicleEntryEvent
import io.gamov.parkflow.events.VehicleType
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import net.datafaker.Faker
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.util.*
import kotlin.random.Random

private val logger = KotlinLogging.logger {}
private val faker = Faker()

@Serializable
data class SimulationConfig(
    val numberOfEvents: Int,
    val delayBetweenEventsMs: Long = 1000,
    val gateIds: List<String> = listOf("GATE_1", "GATE_2"),
    val laneIds: List<String> = listOf("LANE_1", "LANE_2")
)

data class KafkaConfig(
    val bootstrapServers: List<String>,
    val topic: String,
    val properties: Map<String, String>,
    val schemaRegistry: SchemaRegistryConfig,
    val producer: ProducerConfig
) {
    data class SchemaRegistryConfig(
        val url: String,
        val properties: Map<String, String> = emptyMap()
    )

    data class ProducerConfig(
        val clientId: String,
        val keySerializer: String,
        val valueSerializer: String,
        val properties: Map<String, String> = emptyMap()
    )

    companion object {
        fun fromApplicationConfig(config: ApplicationConfig): KafkaConfig {
            return KafkaConfig(
                bootstrapServers = config.property("kafka.bootstrapServers").getList(),
                topic = config.property("kafka.topic").getString(),
                properties = config.config("kafka.properties").keys().associateWith { key ->
                    config.property("kafka.properties.$key").getString()
                },
                schemaRegistry = SchemaRegistryConfig(
                    url = config.property("kafka.schemaRegistry.url").getString(),
                    properties = config.config("kafka.schemaRegistry.properties").keys().associateWith { key ->
                        config.property("kafka.schemaRegistry.properties.$key").getString()
                    }
                ),
                producer = ProducerConfig(
                    clientId = config.property("kafka.producer.clientId").getString(),
                    keySerializer = config.property("kafka.producer.keySerializer").getString(),
                    valueSerializer = config.property("kafka.producer.valueSerializer").getString(),
                    properties = config.config("kafka.producer.properties").keys().associateWith { key ->
                        config.property("kafka.producer.properties.$key").getString()
                    }
                )
            )
        }
    }
}

class EntrySimulator(private val kafkaConfig: KafkaConfig) {
    private val producer = createProducer(kafkaConfig)

    private fun createProducer(config: KafkaConfig): KafkaProducer<String, VehicleEntryEvent> {
        val props = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers.joinToString(","))
            put(ProducerConfig.CLIENT_ID_CONFIG, config.producer.clientId)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, config.producer.keySerializer)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, config.producer.valueSerializer)

            // Add Schema Registry configuration
            put("schema.registry.url", config.schemaRegistry.url)
            config.schemaRegistry.properties.forEach { (key, value) -> put(key, value) }

            // Add all Kafka properties
            config.properties.forEach { (key, value) -> put(key, value) }

            // Add all producer properties
            config.producer.properties.forEach { (key, value) -> put(key, value) }
        }
        return KafkaProducer(props)
    }

    fun generateEvent(gateId: String, laneId: String): VehicleEntryEvent {
        return VehicleEntryEvent().apply {
            eventId = UUID.randomUUID().toString()
            timestamp = Instant.now().toEpochMilli()
            licensePlate = faker.vehicle().licensePlate()
            this.gateId = gateId
            this.laneId = laneId
            confidence = Random.nextDouble(0.7, 1.0)
            imageUrl = null
            vehicleType = VehicleType.values()[Random.nextInt(VehicleType.values().size)]
        }
    }

    suspend fun sendEvent(event: VehicleEntryEvent) {
        withContext(Dispatchers.IO) {
            producer.send(ProducerRecord(kafkaConfig.topic, event.eventId, event)).get()
            logger.info { "Sent event: ${event.eventId} for vehicle ${event.licensePlate}" }
        }
    }

    suspend fun runSimulation(config: SimulationConfig) {
        repeat(config.numberOfEvents) { i ->
            val gateId = config.gateIds.random()
            val laneId = config.laneIds.random()
            val event = generateEvent(gateId, laneId)
            sendEvent(event)
            if (i < config.numberOfEvents - 1) {
                delay(config.delayBetweenEventsMs)
            }
        }
    }
}

fun main() {
    val kafkaConfig = KafkaConfig(
        bootstrapServers = listOf(System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:29092"),
        topic = System.getenv("KAFKA_TOPIC") ?: "parking.entry.events",
        properties = mapOf("security.protocol" to "PLAINTEXT"),
        schemaRegistry = KafkaConfig.SchemaRegistryConfig(
            url = System.getenv("SCHEMA_REGISTRY_URL") ?: "http://localhost:8081",
            properties = mapOf(
                "schema.registry.url" to (System.getenv("SCHEMA_REGISTRY_URL") ?: "http://localhost:8081"),
                "auto.register.schemas" to "true",
                "use.latest.version" to "true"
            )
        ),
        producer = KafkaConfig.ProducerConfig(
            clientId = "parkflow-entry-producer",
            keySerializer = "org.apache.kafka.common.serialization.StringSerializer",
            valueSerializer = "io.confluent.kafka.serializers.KafkaAvroSerializer",
            properties = mapOf(
                "acks" to "all",
                "retries" to "3",
                "retry.backoff.ms" to "1000",
                "max.block.ms" to "10000"
            )
        )
    )

    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8085,
        host = System.getenv("HOST") ?: "0.0.0.0"
    ) {
        val simulator = EntrySimulator(kafkaConfig)

        install(ContentNegotiation) {
            json()
        }

        routing {
            entryRoutes(simulator)
        }
    }.start(wait = true)
}
